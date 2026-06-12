package dev.mattramotar.meeseeks.runtime.internal

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import dev.mattramotar.meeseeks.runtime.AppContext
import dev.mattramotar.meeseeks.runtime.BGTaskManager
import dev.mattramotar.meeseeks.runtime.BGTaskManagerConfig
import dev.mattramotar.meeseeks.runtime.ScheduledTask
import dev.mattramotar.meeseeks.runtime.TaskEvent
import dev.mattramotar.meeseeks.runtime.TaskEventOutcome
import dev.mattramotar.meeseeks.runtime.TaskEventReplay
import dev.mattramotar.meeseeks.runtime.TaskId
import dev.mattramotar.meeseeks.runtime.TaskRequest
import dev.mattramotar.meeseeks.runtime.TaskResult
import dev.mattramotar.meeseeks.runtime.TaskStatus
import dev.mattramotar.meeseeks.runtime.db.MeeseeksDatabase
import dev.mattramotar.meeseeks.runtime.internal.coroutines.MeeseeksDispatchers
import dev.mattramotar.meeseeks.runtime.internal.db.TaskMapper
import dev.mattramotar.meeseeks.runtime.internal.db.model.TaskState
import dev.mattramotar.meeseeks.runtime.internal.db.model.toDbValue
import dev.mattramotar.meeseeks.runtime.internal.db.model.toPublicStatus
import dev.mattramotar.meeseeks.runtime.telemetry.Telemetry
import dev.mattramotar.meeseeks.runtime.telemetry.TelemetryEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext


internal class RealBGTaskManager(
    private val database: MeeseeksDatabase,
    private val workRequestFactory: WorkRequestFactory,
    private val taskScheduler: TaskScheduler,
    private val taskRescheduler: TaskRescheduler,
    internal val config: BGTaskManagerConfig,
    internal val registry: WorkerRegistry,
    private val constraintCapabilities: ConstraintCapabilities,
    private val appContext: AppContext,
    override val coroutineContext: CoroutineContext = SupervisorJob() + MeeseeksDispatchers.IO,
    private val telemetry: Telemetry? = null,
) : BGTaskManager, TaskEventReplay, CoroutineScope {

    private val orphanedTaskWatchdog = OrphanedTaskWatchdog(
        scope = this,
        taskRescheduler = taskRescheduler,
        taskScheduler = taskScheduler,
        registry = registry,
        database = database,
        interval = config.orphanedTaskWatchdogInterval,
        telemetry = telemetry
    )

    val dependencies: MeeseeksDependencies by lazy {
        MeeseeksDependencies(database, registry, config, appContext)
    }

    private val taskSpecQueries = database.taskSpecQueries
    private val taskLogQueries = database.taskLogQueries

    init {
        recoverStuckTasks()

        launch {
            taskRescheduler.rescheduleTasks()
        }

        orphanedTaskWatchdog.start()
    }

    override fun schedule(request: TaskRequest): TaskId {
        constraintCapabilities.validate(request.preconditions, operation = "schedule")

        val timestamp = Timestamp.now()
        val normalized = TaskMapper.normalizeRequest(request, timestamp, registry, config)
        val taskId = newTaskId()

        taskSpecQueries.transaction {
            taskSpecQueries.insertTask(
                id = taskId.value,
                state = normalized.state,
                created_at_ms = timestamp,
                updated_at_ms = timestamp,
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
                backoff_jitter_factor = normalized.backoffJitterFactor
            )
        }

        val workRequest = workRequestFactory.createWorkRequest(taskId.value, request, config)

        taskScheduler.scheduleTask(taskId.value, request, workRequest, ExistingWorkPolicy.KEEP)

        taskSpecQueries.updatePlatformId(
            platform_id = workRequest.id,
            updated_at_ms = Timestamp.now(),
            id = taskId.value
        )

        launch {
            telemetry?.onEvent(
                TelemetryEvent.TaskScheduled(
                    taskId = taskId,
                    task = request
                )
            )
        }

        return taskId
    }

    override fun cancel(id: TaskId) {
        val taskEntity = taskSpecQueries.selectTaskById(id.value).executeAsOneOrNull() ?: return

        val platformId = taskEntity.platform_id
        if (!platformId.isNullOrEmpty()) {
            val scheduleType = taskEntity.schedule_type
            val schedule = when (scheduleType) {
                "ONE_TIME" -> TaskMapper.mapToTaskRequest(taskEntity, registry).schedule
                "PERIODIC" -> TaskMapper.mapToTaskRequest(taskEntity, registry).schedule
                else -> TaskMapper.mapToTaskRequest(taskEntity, registry).schedule
            }
            taskScheduler.cancelWorkById(platformId, schedule)
        } else {
            val taskRequest = TaskMapper.mapToTaskRequest(taskEntity, registry)
            taskScheduler.cancelUniqueWork(
                uniqueWorkName = WorkRequestFactory.uniqueWorkNameFor(
                    taskEntity.id,
                    taskRequest.schedule
                ),
                taskRequest.schedule
            )
        }

        if (!cancelInDatabase(id.value, taskEntity.run_attempt_count, "Task cancelled.")) {
            return
        }

        launch {
            telemetry?.onEvent(
                TelemetryEvent.TaskCancelled(
                    taskId = id,
                    task = TaskMapper.mapToTaskRequest(taskEntity, registry)
                )
            )
        }
    }

    override fun cancelAll() {
        taskScheduler.cancelAllWorkByTag(WorkRequestFactory.WORK_REQUEST_TAG)

        // Includes RUNNING tasks: an in-flight attempt cannot be interrupted,
        // but cancelling it in the database means its completion loses to the
        // cancellation and recoverStuckTasks() will not resurrect it.
        val activeTasks = taskSpecQueries.selectAllCancellable().executeAsList()

        val cancelledTasks = activeTasks.filter { entity ->
            cancelInDatabase(entity.id, entity.run_attempt_count, "Task cancelled by cancelAll().")
        }

        cancelledTasks.forEach { entity ->
            launch {
                telemetry?.onEvent(
                    TelemetryEvent.TaskCancelled(
                        taskId = TaskId(entity.id),
                        task = TaskMapper.mapToTaskRequest(entity, registry)
                    )
                )
            }
        }
    }

    /**
     * Atomically cancels a task unless it already reached a terminal state, and
     * logs the durable Cancelled event and clears checkpoints only when this
     * call performed the cancellation. Returns false for tasks that were
     * already terminal.
     */
    private fun cancelInDatabase(taskId: String, attempt: Long, message: String): Boolean {
        val cancelledAt = Timestamp.now()
        return taskSpecQueries.transactionWithResult {
            taskSpecQueries.cancelTaskIfActive(cancelledAt, taskId)
            val cancelled = taskSpecQueries.selectChanges().executeAsOne() > 0L
            if (cancelled) {
                taskLogQueries.insertLog(
                    taskId = taskId,
                    created = cancelledAt,
                    result = TaskResult.Type.Cancelled,
                    attempt = attempt,
                    message = message
                )
                database.taskCheckpointQueries.deleteAllCheckpointsForTask(taskId)
            }
            cancelled
        }
    }

    override fun reschedulePendingTasks() {
        val activeTasks = taskSpecQueries.selectAllActive().executeAsList()
        activeTasks.forEach { taskSpec ->
            if (taskSpec.platform_id.isNullOrBlank()) {
                taskRescheduler.rescheduleTask(taskSpec)
            }
        }
    }

    override fun getTaskStatus(id: TaskId): TaskStatus? {
        val row = taskSpecQueries
            .selectTaskById(id.value)
            .executeAsOneOrNull() ?: return null
        return TaskState.fromDbValue(row.state).toPublicStatus()
    }

    override fun listTasks(): List<ScheduledTask> {
        return taskSpecQueries.selectAllTasks()
            .executeAsList()
            .map { TaskMapper.mapToScheduledTask(it, registry) }
    }

    override fun reschedule(
        id: TaskId,
        updatedRequest: TaskRequest
    ): TaskId {
        constraintCapabilities.validate(updatedRequest.preconditions, operation = "reschedule")

        val existing = taskSpecQueries.selectTaskById(id.value).executeAsOneOrNull()
            ?: error("Update failed: Task $id not found.")

        val existingPlatformId = existing.platform_id
        if (!existingPlatformId.isNullOrEmpty()) {
            val existingRequest = TaskMapper.mapToTaskRequest(existing, registry)
            taskScheduler.cancelWorkById(existingPlatformId, existingRequest.schedule)
        } else {
            val existingRequest = TaskMapper.mapToTaskRequest(existing, registry)
            taskScheduler.cancelUniqueWork(
                uniqueWorkName = WorkRequestFactory.uniqueWorkNameFor(
                    existing.id,
                    existingRequest.schedule
                ),
                existingRequest.schedule
            )
        }

        val timestamp = Timestamp.now()
        val normalized = TaskMapper.normalizeRequest(updatedRequest, timestamp, registry, config)

        taskSpecQueries.updateTask(
            state = TaskState.ENQUEUED.toDbValue(),
            payload_type_id = normalized.payloadTypeId,
            payload_data = normalized.payloadData,
            priority = normalized.priority,
            requires_network = normalized.requiresNetwork,
            requires_charging = normalized.requiresCharging,
            requires_battery_not_low = normalized.requiresBatteryNotLow,
            schedule_type = normalized.scheduleType,
            next_run_time_ms = normalized.nextRunTimeMs,
            initial_delay_ms = normalized.initialDelayMs,
            interval_duration_ms = normalized.intervalMs,
            flex_duration_ms = normalized.flexMs,
            backoff_policy = normalized.backoffPolicy,
            backoff_delay_ms = normalized.backoffDelayMs,
            max_retries = normalized.maxRetries,
            backoff_multiplier = normalized.backoffMultiplier,
            backoff_jitter_factor = normalized.backoffJitterFactor,
            platform_id = null,
            updated_at_ms = timestamp,
            id = existing.id
        )

        val newWorkRequest = workRequestFactory.createWorkRequest(existing.id, updatedRequest, config)
        taskScheduler.scheduleTask(
            existing.id,
            updatedRequest,
            newWorkRequest,
            ExistingWorkPolicy.KEEP
        )

        taskSpecQueries.updatePlatformId(newWorkRequest.id, timestamp, existing.id)

        launch {
            telemetry?.onEvent(
                TelemetryEvent.TaskScheduled(
                    taskId = TaskId(existing.id),
                    task = updatedRequest
                )
            )
        }

        return id
    }

    override fun observeStatus(id: TaskId): Flow<TaskStatus?> {
        return taskSpecQueries
            .selectTaskById(id.value)
            .asFlow()
            .mapToOneOrNull(context = MeeseeksDispatchers.IO)
            .map { entity -> entity?.state?.let { TaskState.fromDbValue(it).toPublicStatus() } }
    }

    override fun getTaskEvents(taskId: TaskId): List<TaskEvent> {
        return terminalEventsForTask(taskId).executeAsList()
    }

    override fun observeTaskEvents(taskId: TaskId): Flow<List<TaskEvent>> {
        return terminalEventsForTask(taskId)
            .asFlow()
            .mapToList(context = MeeseeksDispatchers.IO)
    }

    override fun replayTerminalEvents(sinceEventId: Long): List<TaskEvent> {
        return taskLogQueries.selectTerminalEventsSince(
            sinceEventId = sinceEventId,
            success = TaskResult.Type.Success,
            permanentFailure = TaskResult.Type.PermanentFailure,
            cancelled = TaskResult.Type.Cancelled,
            mapper = ::mapTaskEvent
        ).executeAsList()
    }

    private fun recoverStuckTasks() {
        database.taskSpecQueries.resetRunningTasksToEnqueued(Timestamp.now())
    }

    private fun terminalEventsForTask(taskId: TaskId) = taskLogQueries.selectTerminalEventsForTask(
        taskId = taskId.value,
        success = TaskResult.Type.Success,
        permanentFailure = TaskResult.Type.PermanentFailure,
        cancelled = TaskResult.Type.Cancelled,
        mapper = ::mapTaskEvent
    )

    private fun mapTaskEvent(
        id: Long,
        taskId: String,
        created: Long,
        result: TaskResult.Type,
        attempt: Long,
        message: String?
    ): TaskEvent {
        return TaskEvent(
            id = id,
            taskId = TaskId(taskId),
            outcome = result.toEventOutcome(),
            createdAt = created,
            attempt = attempt.toInt(),
            message = message
        )
    }

    private fun TaskResult.Type.toEventOutcome(): TaskEventOutcome {
        return when (this) {
            TaskResult.Type.Success -> TaskEventOutcome.Success
            TaskResult.Type.PermanentFailure -> TaskEventOutcome.Failure
            TaskResult.Type.Cancelled -> TaskEventOutcome.Cancelled
            TaskResult.Type.TransientFailure,
            TaskResult.Type.Retry,
            TaskResult.Type.SuccessAndScheduledNext ->
                error("Non-terminal log type $this should never match a terminal event query.")
        }
    }
}
