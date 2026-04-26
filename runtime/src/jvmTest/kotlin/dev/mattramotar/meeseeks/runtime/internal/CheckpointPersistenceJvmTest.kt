package dev.mattramotar.meeseeks.runtime.internal

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import dev.mattramotar.meeseeks.runtime.AppContext
import dev.mattramotar.meeseeks.runtime.BGTaskManagerConfig
import dev.mattramotar.meeseeks.runtime.CheckpointDecodeException
import dev.mattramotar.meeseeks.runtime.CheckpointIncompatibleException
import dev.mattramotar.meeseeks.runtime.CheckpointStore
import dev.mattramotar.meeseeks.runtime.CheckpointedWorker
import dev.mattramotar.meeseeks.runtime.RuntimeContext
import dev.mattramotar.meeseeks.runtime.TaskId
import dev.mattramotar.meeseeks.runtime.TaskPayload
import dev.mattramotar.meeseeks.runtime.TaskRequest
import dev.mattramotar.meeseeks.runtime.TaskResult
import dev.mattramotar.meeseeks.runtime.TaskRetryPolicy
import dev.mattramotar.meeseeks.runtime.Worker
import dev.mattramotar.meeseeks.runtime.WorkerFactory
import dev.mattramotar.meeseeks.runtime.db.MeeseeksDatabase
import dev.mattramotar.meeseeks.runtime.db.TaskSpec
import dev.mattramotar.meeseeks.runtime.internal.db.TaskMapper
import dev.mattramotar.meeseeks.runtime.internal.db.adapters.taskLogEntityAdapter
import dev.mattramotar.meeseeks.runtime.read
import dev.mattramotar.meeseeks.runtime.write
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.quartz.Scheduler
import org.quartz.impl.StdSchedulerFactory
import java.util.Properties
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalSerializationApi::class)
class CheckpointPersistenceJvmTest {

    private object TestAppContext : AppContext()

    @Serializable
    private data class ResumePayload(val id: String) : TaskPayload

    @Serializable
    private data class Progress(val completedStep: Int)

    @Serializable
    private data class MultiStepCheckpoint(val completedThrough: Int = 0)

    @Serializable
    private data class OtherProgress(val value: String)

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @Test
    fun checkpointedWorkerReadsCheckpointOnRetryAndSuccessClearsIt() = runTest {
        val database = database()
        val observed = mutableListOf<Progress?>()
        val registry = registry { appContext -> ResumableWorker(appContext, observed) }
        val taskId = "retry-resume-task"

        insertTask(database, registry, taskId)

        val firstResult = TaskExecutor.execute(
            taskId = taskId,
            database = database,
            registry = registry,
            appContext = TestAppContext,
            config = BGTaskManagerConfig()
        )

        assertIs<TaskExecutor.ExecutionResult.ScheduleNextActivation>(firstResult)
        assertEquals(1L, checkpointCount(database, taskId))

        val restartedRegistry = registry { appContext -> ResumableWorker(appContext, observed) }
        val secondResult = TaskExecutor.execute(
            taskId = taskId,
            database = database,
            registry = restartedRegistry,
            appContext = TestAppContext,
            config = BGTaskManagerConfig()
        )

        assertEquals(TaskExecutor.ExecutionResult.Terminal.Success, secondResult)
        assertEquals(listOf(null, Progress(1)), observed)
        assertEquals(0L, checkpointCount(database, taskId))
    }

    @Test
    fun multiStepWorkerSkipsCompletedStepsAfterManagerRestart() = runTest {
        val database = database()
        val sideEffects = mutableListOf<String>()
        val taskId = "multi-step-resume-task"
        val firstRegistry = registry { appContext ->
            MultiStepWorker(
                appContext = appContext,
                sideEffects = sideEffects,
                failAfterCompletedThrough = 2,
            )
        }

        insertTask(database, firstRegistry, taskId)

        val firstResult = TaskExecutor.execute(
            taskId = taskId,
            database = database,
            registry = firstRegistry,
            appContext = TestAppContext,
            config = BGTaskManagerConfig()
        )

        assertIs<TaskExecutor.ExecutionResult.ScheduleNextActivation>(firstResult)
        assertEquals(listOf("download", "transform"), sideEffects)
        assertEquals(1L, checkpointCount(database, taskId))

        val restartedRegistry = registry { appContext ->
            MultiStepWorker(
                appContext = appContext,
                sideEffects = sideEffects,
                failAfterCompletedThrough = null,
            )
        }
        val secondResult = TaskExecutor.execute(
            taskId = taskId,
            database = database,
            registry = restartedRegistry,
            appContext = TestAppContext,
            config = BGTaskManagerConfig()
        )

        assertEquals(TaskExecutor.ExecutionResult.Terminal.Success, secondResult)
        assertEquals(listOf("download", "transform", "upload"), sideEffects)
        assertEquals(1, sideEffects.count { it == "download" })
        assertEquals(1, sideEffects.count { it == "transform" })
        assertEquals(1, sideEffects.count { it == "upload" })
        assertEquals(0L, checkpointCount(database, taskId))
    }

    @Test
    fun checkpointsSurviveTransientFailureAndClearOnPermanentFailure() = runTest {
        val database = database()
        val taskId = "terminal-cleanup-task"
        val transientRegistry = registry { appContext ->
            ResultWorker(appContext, TaskResult.Failure.Transient(RuntimeException("retry")))
        }

        insertTask(database, transientRegistry, taskId)

        val transientResult = TaskExecutor.execute(
            taskId = taskId,
            database = database,
            registry = transientRegistry,
            appContext = TestAppContext,
            config = BGTaskManagerConfig()
        )

        assertIs<TaskExecutor.ExecutionResult.ScheduleNextActivation>(transientResult)
        assertEquals(1L, checkpointCount(database, taskId))

        val permanentRegistry = registry { appContext ->
            ResultWorker(appContext, TaskResult.Failure.Permanent(RuntimeException("done")))
        }
        val permanentResult = TaskExecutor.execute(
            taskId = taskId,
            database = database,
            registry = permanentRegistry,
            appContext = TestAppContext,
            config = BGTaskManagerConfig()
        )

        assertEquals(TaskExecutor.ExecutionResult.Terminal.Failure, permanentResult)
        assertEquals(0L, checkpointCount(database, taskId))
    }

    @Test
    fun cancelClearsCheckpoints() = runTest {
        val database = database()
        val registry = registry { appContext -> ResultWorker(appContext, TaskResult.Success) }
        val scheduler = createInMemoryScheduler()

        try {
            val manager = manager(database, registry, scheduler)
            val taskId = "cancel-cleanup-task"
            insertTask(database, registry, taskId)
            val store = checkpointStore(database, registry, taskId)

            store.write(Progress(1), Progress.serializer(), "cursor")

            assertEquals(1L, checkpointCount(database, taskId))

            manager.cancel(TaskId(taskId))

            assertEquals(0L, checkpointCount(database, taskId))
        } finally {
            scheduler.shutdown(true)
        }
    }

    @Test
    fun checkpointStoreOverwritesAndClearsKeys() = runTest {
        val database = database()
        val registry = registry { appContext -> ResultWorker(appContext, TaskResult.Success) }
        val taskId = "store-overwrite-task"
        insertTask(database, registry, taskId)
        val store = checkpointStore(database, registry, taskId)

        store.write(Progress(1), Progress.serializer(), "cursor")
        store.write(Progress(2), Progress.serializer(), "cursor")
        store.write(Progress(3), Progress.serializer(), "side")

        assertEquals(Progress(2), store.read(Progress.serializer(), "cursor"))
        assertEquals(2L, checkpointCount(database, taskId))

        store.clear("cursor")

        assertNull(store.read(Progress.serializer(), "cursor"))
        assertEquals(1L, checkpointCount(database, taskId))

        store.clearAll()

        assertEquals(0L, checkpointCount(database, taskId))
    }

    @Test
    fun checkpointStoreReportsIncompatibleAndCorruptData() = runTest {
        val database = database()
        val registry = registry { appContext -> ResultWorker(appContext, TaskResult.Success) }
        val taskId = "store-error-task"
        insertTask(database, registry, taskId)
        val store = checkpointStore(database, registry, taskId)

        store.write(Progress(1), Progress.serializer(), "cursor")

        assertFailsWith<CheckpointIncompatibleException> {
            store.read(OtherProgress.serializer(), "cursor")
        }

        database.taskCheckpointQueries.updateCheckpoint(
            payload_type_id = payloadTypeId(),
            worker_type_id = payloadTypeId(),
            checkpoint_type_id = registry.checkpointTypeId(Progress.serializer()),
            data_ = "not-json",
            updated_at_ms = 1L,
            task_id = taskId,
            checkpoint_key = "cursor",
        )

        assertFailsWith<CheckpointDecodeException> {
            store.read(Progress.serializer(), "cursor")
        }
    }

    private class ResumableWorker(
        appContext: AppContext,
        private val observed: MutableList<Progress?>,
    ) : CheckpointedWorker<ResumePayload>(appContext) {

        override suspend fun run(
            payload: ResumePayload,
            context: RuntimeContext,
            checkpoints: CheckpointStore,
        ): TaskResult {
            val progress = checkpoints.read<Progress>()
            observed += progress
            return if (progress == null) {
                checkpoints.write(Progress(1))
                TaskResult.Retry
            } else {
                TaskResult.Success
            }
        }
    }

    private class MultiStepWorker(
        appContext: AppContext,
        private val sideEffects: MutableList<String>,
        private val failAfterCompletedThrough: Int?,
    ) : Worker<ResumePayload>(appContext), CheckpointedWorker<ResumePayload> {

        private val steps = listOf("download", "transform", "upload")

        override suspend fun run(payload: ResumePayload, context: RuntimeContext): TaskResult {
            error("Checkpointed workers must use the checkpoint-aware run method.")
        }

        override suspend fun run(
            payload: ResumePayload,
            context: RuntimeContext,
            checkpoints: CheckpointStore,
        ): TaskResult {
            val checkpoint = checkpoints.read<MultiStepCheckpoint>() ?: MultiStepCheckpoint()

            for (index in checkpoint.completedThrough until steps.size) {
                sideEffects += steps[index]
                val completedThrough = index + 1
                checkpoints.write(MultiStepCheckpoint(completedThrough = completedThrough))

                if (completedThrough == failAfterCompletedThrough) {
                    return TaskResult.Failure.Transient(RuntimeException("simulated process death"))
                }
            }

            return TaskResult.Success
        }
    }

    private class ResultWorker(
        appContext: AppContext,
        private val result: TaskResult,
    ) : CheckpointedWorker<ResumePayload>(appContext) {

        override suspend fun run(
            payload: ResumePayload,
            context: RuntimeContext,
            checkpoints: CheckpointStore,
        ): TaskResult {
            checkpoints.write(Progress(context.attemptCount))
            return result
        }
    }

    private fun registry(factory: (AppContext) -> Worker<ResumePayload>): WorkerRegistry {
        val serializer = ResumePayload.serializer()
        val registration = WorkerRegistration(
            type = ResumePayload::class,
            typeId = serializer.descriptor.serialName,
            serializer = serializer,
            factory = WorkerFactory(factory)
        )
        return WorkerRegistry(
            registrations = mapOf(ResumePayload::class to registration),
            json = json
        )
    }

    private fun database(): MeeseeksDatabase {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        MeeseeksDatabase.Schema.create(driver)
        return MeeseeksDatabase(driver, taskLogEntityAdapter(json))
    }

    private fun insertTask(
        database: MeeseeksDatabase,
        registry: WorkerRegistry,
        taskId: String,
    ) {
        val normalized = TaskMapper.normalizeRequest(
            request = TaskRequest(
                payload = ResumePayload(taskId),
                retryPolicy = TaskRetryPolicy.FixedInterval(
                    retryInterval = 1.seconds,
                    maxRetries = 3,
                ),
            ),
            currentTimeMs = 0L,
            registry = registry,
            config = BGTaskManagerConfig()
        )

        database.taskSpecQueries.insertTask(
            id = taskId,
            state = normalized.state,
            created_at_ms = 0L,
            updated_at_ms = 0L,
            platform_id = null,
            payload_type_id = normalized.payloadTypeId,
            payload_data = normalized.payloadData,
            priority = normalized.priority,
            requires_network = normalized.requiresNetwork,
            requires_charging = normalized.requiresCharging,
            requires_battery_not_low = normalized.requiresBatteryNotLow,
            next_run_time_ms = normalized.nextRunTimeMs,
            schedule_type = normalized.scheduleType,
            initial_delay_ms = normalized.initialDelayMs,
            interval_duration_ms = normalized.intervalMs,
            flex_duration_ms = normalized.flexMs,
            backoff_policy = normalized.backoffPolicy,
            backoff_delay_ms = normalized.backoffDelayMs,
            max_retries = normalized.maxRetries,
            backoff_multiplier = normalized.backoffMultiplier,
            backoff_jitter_factor = normalized.backoffJitterFactor,
        )
    }

    private fun checkpointStore(
        database: MeeseeksDatabase,
        registry: WorkerRegistry,
        taskId: String,
    ): RealCheckpointStore {
        return RealCheckpointStore(
            database = database,
            registry = registry,
            taskId = taskId,
            payloadTypeId = payloadTypeId(),
            workerTypeId = payloadTypeId(),
        )
    }

    private fun checkpointCount(database: MeeseeksDatabase, taskId: String): Long {
        return database.taskCheckpointQueries.countCheckpointsForTask(taskId).executeAsOne()
    }

    private fun payloadTypeId(): String {
        return ResumePayload.serializer().descriptor.serialName
    }

    private fun manager(
        database: MeeseeksDatabase,
        registry: WorkerRegistry,
        scheduler: Scheduler,
    ): RealBGTaskManager {
        val taskScheduler = TaskScheduler(scheduler)
        val workRequestFactory = WorkRequestFactory()
        val config = BGTaskManagerConfig(orphanedTaskWatchdogInterval = Duration.ZERO)
        return RealBGTaskManager(
            database = database,
            workRequestFactory = workRequestFactory,
            taskScheduler = taskScheduler,
            taskRescheduler = NoOpTaskRescheduler,
            config = config,
            registry = registry,
            constraintCapabilities = ConstraintCapabilities.JVM,
            appContext = TestAppContext,
        )
    }

    private fun createInMemoryScheduler(): Scheduler {
        val properties = Properties().apply {
            put("org.quartz.scheduler.instanceName", "meeseeks-checkpoint-test-${UUID.randomUUID()}")
            put("org.quartz.scheduler.instanceId", "AUTO")
            put("org.quartz.threadPool.class", "org.quartz.simpl.SimpleThreadPool")
            put("org.quartz.threadPool.threadCount", "1")
            put("org.quartz.jobStore.class", "org.quartz.simpl.RAMJobStore")
        }

        return StdSchedulerFactory(properties).scheduler.apply {
            start()
        }
    }

    private object NoOpTaskRescheduler : TaskRescheduler {
        override fun rescheduleTasks() = Unit

        override fun rescheduleTask(taskSpec: TaskSpec) = Unit
    }
}
