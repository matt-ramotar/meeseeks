package dev.mattramotar.meeseeks.runtime.internal

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import dev.mattramotar.meeseeks.runtime.AppContext
import dev.mattramotar.meeseeks.runtime.BGTaskManager
import dev.mattramotar.meeseeks.runtime.BGTaskManagerConfig
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
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalSerializationApi::class)
class TaskEventReplayJvmTest {

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
    fun successEventCanBeRecoveredByRestartedManager() = runTest {
        val database = createDatabase()
        val registry = registry(TaskResult.Success)
        val manager = createManager(database, registry)
        val taskId = manager.schedule(TaskRequest(TestPayload("success")))

        TaskExecutor.execute(taskId.value, database, registry, TestAppContext, config)

        val restarted: BGTaskManager = createManager(database, registry)
        val event = restarted.getTaskEvents(taskId).single()

        assertEquals(TaskEventOutcome.Success, event.outcome)
        assertEquals(taskId, event.taskId)
        assertEquals(1, event.attempt)
        assertEquals(TaskStatus.Finished.Completed, restarted.getTaskStatus(taskId))
        assertEquals(listOf(event), restarted.replayTerminalEvents(sinceEventId = 0L))
        assertTrue(restarted.replayTerminalEvents(sinceEventId = event.id).isEmpty())
    }

    @Test
    fun permanentFailureEventCanBeRecoveredByRestartedManager() = runTest {
        val database = createDatabase()
        val registry = registry(TaskResult.Failure.Permanent(IllegalStateException("boom")))
        val manager = createManager(database, registry)
        val taskId = manager.schedule(TaskRequest(TestPayload("failure")))

        TaskExecutor.execute(taskId.value, database, registry, TestAppContext, config)

        val restarted: BGTaskManager = createManager(database, registry)
        val event = restarted.getTaskEvents(taskId).single()

        assertEquals(TaskEventOutcome.Failure, event.outcome)
        assertEquals(taskId, event.taskId)
        assertEquals(1, event.attempt)
        assertTrue(event.message?.contains("Permanent failure") == true)
        assertEquals(TaskStatus.Finished.Failed, restarted.getTaskStatus(taskId))
    }

    @Test
    fun cancellationEventCanBeRecoveredByRestartedManager() {
        val database = createDatabase()
        val registry = registry(TaskResult.Success)
        val manager = createManager(database, registry)
        val taskId = manager.schedule(TaskRequest(TestPayload("cancel")))

        manager.cancel(taskId)

        val restarted: BGTaskManager = createManager(database, registry)
        val event = restarted.getTaskEvents(taskId).single()

        assertEquals(TaskEventOutcome.Cancelled, event.outcome)
        assertEquals(taskId, event.taskId)
        assertEquals(0, event.attempt)
        assertEquals(TaskStatus.Finished.Cancelled, restarted.getTaskStatus(taskId))
    }

    @Test
    fun periodicSuccessIsNotATerminalReplayEvent() = runTest {
        val database = createDatabase()
        val registry = registry(TaskResult.Success)
        val manager = createManager(database, registry)
        val taskId = manager.schedule(
            TaskRequest(
                payload = TestPayload("periodic"),
                schedule = TaskSchedule.Periodic(interval = 5.seconds)
            )
        )

        val execution = TaskExecutor.execute(taskId.value, database, registry, TestAppContext, config)

        val restarted: BGTaskManager = createManager(database, registry)
        assertTrue(execution is TaskExecutor.ExecutionResult.ScheduleNextActivation)
        assertTrue(restarted.getTaskEvents(taskId).isEmpty())
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

    private fun registry(result: TaskResult): WorkerRegistry {
        val serializer = TestPayload.serializer()
        val registration = WorkerRegistration(
            type = TestPayload::class,
            typeId = serializer.descriptor.serialName,
            serializer = serializer,
            factory = WorkerFactory<TestPayload> { appContext ->
                object : Worker<TestPayload>(appContext) {
                    override suspend fun run(payload: TestPayload, context: dev.mattramotar.meeseeks.runtime.RuntimeContext): TaskResult {
                        return result
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
            put("org.quartz.scheduler.instanceName", "meeseeks-event-replay-test-${UUID.randomUUID()}")
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
