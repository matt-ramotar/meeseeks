package dev.mattramotar.meeseeks.runtime.internal

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import dev.mattramotar.meeseeks.runtime.AppContext
import dev.mattramotar.meeseeks.runtime.BGTaskManager
import dev.mattramotar.meeseeks.runtime.BGTaskManagerConfig
import dev.mattramotar.meeseeks.runtime.RuntimeContext
import dev.mattramotar.meeseeks.runtime.TaskEventOutcome
import dev.mattramotar.meeseeks.runtime.TaskPayload
import dev.mattramotar.meeseeks.runtime.TaskRequest
import dev.mattramotar.meeseeks.runtime.TaskResult
import dev.mattramotar.meeseeks.runtime.TaskSchedule
import dev.mattramotar.meeseeks.runtime.TaskStatus
import dev.mattramotar.meeseeks.runtime.Worker
import dev.mattramotar.meeseeks.runtime.WorkerFactory
import dev.mattramotar.meeseeks.runtime.db.MeeseeksDatabase
import dev.mattramotar.meeseeks.runtime.getTaskEvents
import dev.mattramotar.meeseeks.runtime.internal.db.adapters.taskLogEntityAdapter
import dev.mattramotar.meeseeks.runtime.replayTerminalEvents
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.quartz.Scheduler
import org.quartz.impl.StdSchedulerFactory
import java.util.Properties
import java.util.UUID
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalSerializationApi::class)
class TaskCancellationJvmTest {

    @Serializable
    private data class TestPayload(val value: String) : TaskPayload

    private object TestAppContext : AppContext()

    private val schedulers = mutableListOf<Scheduler>()
    private val config = BGTaskManagerConfig(orphanedTaskWatchdogInterval = Duration.ZERO)
    private val json = Json

    @AfterTest
    fun tearDown() {
        schedulers.forEach { scheduler ->
            if (!scheduler.isShutdown) {
                scheduler.shutdown(true)
            }
        }
        schedulers.clear()
    }

    @Test
    fun cancelAllCancelsARunningTaskAndRestartDoesNotResurrectIt() = runTest {
        val database = createDatabase()
        var runCount = 0
        val registry = registry {
            runCount++
            TaskResult.Success
        }
        val manager = createManager(database, registry)
        val taskId = manager.schedule(TaskRequest(TestPayload("running")))

        // Simulate an in-flight attempt: the platform claimed the task but the
        // worker has not finished yet.
        database.taskSpecQueries.atomicallyClaimAndStartTask(Timestamp.now(), taskId.value)

        manager.cancelAll()

        assertEquals(TaskStatus.Finished.Cancelled, manager.getTaskStatus(taskId))

        val restarted: BGTaskManager = createManager(database, registry)
        assertEquals(TaskStatus.Finished.Cancelled, restarted.getTaskStatus(taskId))

        // A stale platform wake-up for the cancelled task cannot claim it.
        val execution = TaskExecutor.execute(taskId.value, database, registry, TestAppContext, config)
        assertEquals(TaskExecutor.ExecutionResult.Terminal.Failure, execution)
        assertEquals(0, runCount)

        val event = restarted.getTaskEvents(taskId).single()
        assertEquals(TaskEventOutcome.Cancelled, event.outcome)
        assertEquals(listOf(event), restarted.replayTerminalEvents(sinceEventId = 0L))
    }

    @Test
    fun cancelMidAttemptWinsOverTheAttemptsSuccess() = runTest {
        val database = createDatabase()
        val workerStarted = CompletableDeferred<Unit>()
        val releaseWorker = CompletableDeferred<Unit>()
        val registry = registry {
            workerStarted.complete(Unit)
            releaseWorker.await()
            TaskResult.Success
        }
        val manager = createManager(database, registry)
        val taskId = manager.schedule(TaskRequest(TestPayload("mid-attempt")))

        val execution = async {
            TaskExecutor.execute(taskId.value, database, registry, TestAppContext, config)
        }
        workerStarted.await()

        manager.cancel(taskId)
        releaseWorker.complete(Unit)

        assertEquals(TaskExecutor.ExecutionResult.Terminal.Failure, execution.await())
        assertEquals(TaskStatus.Finished.Cancelled, manager.getTaskStatus(taskId))

        val event = manager.getTaskEvents(taskId).single()
        assertEquals(TaskEventOutcome.Cancelled, event.outcome)

        // The attempt's Success was discarded entirely: the only log row for
        // the task is the Cancelled event.
        val logs = database.taskLogQueries.selectLogsForTask(taskId.value).executeAsList()
        assertEquals(listOf(TaskResult.Type.Cancelled), logs.map { it.result })
    }

    @Test
    fun cancelAllMidAttemptPreventsTheTaskFromRunningAgainAfterRestart() = runTest {
        val database = createDatabase()
        var runCount = 0
        val workerStarted = CompletableDeferred<Unit>()
        val releaseWorker = CompletableDeferred<Unit>()
        val registry = registry {
            runCount++
            if (runCount == 1) {
                workerStarted.complete(Unit)
                releaseWorker.await()
            }
            TaskResult.Success
        }
        val manager = createManager(database, registry)
        val taskId = manager.schedule(TaskRequest(TestPayload("cancel-all")))

        val execution = async {
            TaskExecutor.execute(taskId.value, database, registry, TestAppContext, config)
        }
        workerStarted.await()

        manager.cancelAll()
        releaseWorker.complete(Unit)
        assertEquals(TaskExecutor.ExecutionResult.Terminal.Failure, execution.await())

        // Restart runs recoverStuckTasks(), which must not resurrect the task.
        val restarted: BGTaskManager = createManager(database, registry)
        assertEquals(TaskStatus.Finished.Cancelled, restarted.getTaskStatus(taskId))

        TaskExecutor.execute(taskId.value, database, registry, TestAppContext, config)
        assertEquals(1, runCount)

        val event = restarted.getTaskEvents(taskId).single()
        assertEquals(TaskEventOutcome.Cancelled, event.outcome)
        assertEquals(listOf(event), restarted.replayTerminalEvents(sinceEventId = 0L))
    }

    @Test
    fun periodicTaskCancelledMidAttemptIsNotReenqueued() = runTest {
        val database = createDatabase()
        val workerStarted = CompletableDeferred<Unit>()
        val releaseWorker = CompletableDeferred<Unit>()
        val registry = registry {
            workerStarted.complete(Unit)
            releaseWorker.await()
            TaskResult.Success
        }
        val manager = createManager(database, registry)
        val taskId = manager.schedule(
            TaskRequest(
                payload = TestPayload("periodic"),
                schedule = TaskSchedule.Periodic(interval = 5.seconds)
            )
        )

        val execution = async {
            TaskExecutor.execute(taskId.value, database, registry, TestAppContext, config)
        }
        workerStarted.await()

        manager.cancel(taskId)
        releaseWorker.complete(Unit)

        assertEquals(TaskExecutor.ExecutionResult.Terminal.Failure, execution.await())
        assertEquals(TaskStatus.Finished.Cancelled, manager.getTaskStatus(taskId))

        val logs = database.taskLogQueries.selectLogsForTask(taskId.value).executeAsList()
        assertEquals(listOf(TaskResult.Type.Cancelled), logs.map { it.result })
    }

    @Test
    fun transientFailureMidAttemptCancelledDoesNotScheduleARetry() = runTest {
        val database = createDatabase()
        val workerStarted = CompletableDeferred<Unit>()
        val releaseWorker = CompletableDeferred<Unit>()
        val registry = registry {
            workerStarted.complete(Unit)
            releaseWorker.await()
            TaskResult.Failure.Transient(IllegalStateException("flaky"))
        }
        val manager = createManager(database, registry)
        val taskId = manager.schedule(TaskRequest(TestPayload("transient")))

        val execution = async {
            TaskExecutor.execute(taskId.value, database, registry, TestAppContext, config)
        }
        workerStarted.await()

        manager.cancel(taskId)
        releaseWorker.complete(Unit)

        assertEquals(TaskExecutor.ExecutionResult.Terminal.Failure, execution.await())
        assertEquals(TaskStatus.Finished.Cancelled, manager.getTaskStatus(taskId))

        val logs = database.taskLogQueries.selectLogsForTask(taskId.value).executeAsList()
        assertEquals(listOf(TaskResult.Type.Cancelled), logs.map { it.result })
    }

    private fun createDatabase(): MeeseeksDatabase {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY, schema = MeeseeksDatabase.Schema)
        return MeeseeksDatabase(driver, taskLogEntityAdapter(json))
    }

    private fun createManager(
        database: MeeseeksDatabase,
        registry: WorkerRegistry
    ): BGTaskManager {
        val scheduler = createInMemoryScheduler()
        schedulers += scheduler
        val taskScheduler = TaskScheduler(scheduler)
        val workRequestFactory = WorkRequestFactory()
        val taskRescheduler = TaskRescheduler(database, taskScheduler, workRequestFactory, config, registry)

        return RealBGTaskManager(
            database = database,
            workRequestFactory = workRequestFactory,
            taskScheduler = taskScheduler,
            taskRescheduler = taskRescheduler,
            config = config,
            registry = registry,
            constraintCapabilities = ConstraintCapabilities.JVM,
            appContext = TestAppContext
        )
    }

    private fun registry(work: suspend () -> TaskResult): WorkerRegistry {
        val serializer = TestPayload.serializer()
        val registration = WorkerRegistration(
            type = TestPayload::class,
            typeId = serializer.descriptor.serialName,
            serializer = serializer,
            factory = WorkerFactory<TestPayload> { appContext ->
                object : Worker<TestPayload>(appContext) {
                    override suspend fun run(payload: TestPayload, context: RuntimeContext): TaskResult {
                        return work()
                    }
                }
            }
        )
        return WorkerRegistry(
            registrations = mapOf(TestPayload::class to registration),
            json = json
        )
    }

    private fun createInMemoryScheduler(): Scheduler {
        val properties = Properties().apply {
            put("org.quartz.scheduler.instanceName", "meeseeks-cancellation-test-${UUID.randomUUID()}")
            put("org.quartz.scheduler.instanceId", "AUTO")
            put("org.quartz.threadPool.class", "org.quartz.simpl.SimpleThreadPool")
            put("org.quartz.threadPool.threadCount", "1")
            put("org.quartz.jobStore.class", "org.quartz.simpl.RAMJobStore")
        }

        return StdSchedulerFactory(properties).scheduler.apply {
            start()
        }
    }
}
