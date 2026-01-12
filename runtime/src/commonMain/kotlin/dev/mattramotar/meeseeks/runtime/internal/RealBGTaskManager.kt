package dev.mattramotar.meeseeks.runtime.internal

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOneOrNull
import dev.mattramotar.meeseeks.runtime.AppContext
import dev.mattramotar.meeseeks.runtime.BGTaskManager
import dev.mattramotar.meeseeks.runtime.BGTaskManagerConfig
import dev.mattramotar.meeseeks.runtime.ScheduledTask
import dev.mattramotar.meeseeks.runtime.TaskId
import dev.mattramotar.meeseeks.runtime.TaskRequest
import dev.mattramotar.meeseeks.runtime.TaskStatus
import dev.mattramotar.meeseeks.runtime.db.MeeseeksDatabase
import dev.mattramotar.meeseeks.runtime.internal.coroutines.MeeseeksDispatchers
import dev.mattramotar.meeseeks.runtime.internal.db.TaskMapper
import dev.mattramotar.meeseeks.runtime.internal.db.model.TaskState
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
    private val appContext: AppContext,
    override val coroutineContext: CoroutineContext = SupervisorJob() + MeeseeksDispatchers.IO,
    private val telemetry: Telemetry? = null,
) : BGTaskManager, CoroutineScope {

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

    init {
        recoverStuckTasks()

        launch {
            taskRescheduler.rescheduleTasks()
        }

        orphanedTaskWatchdog.start()
    }

    override fun schedule(request: TaskRequest): TaskId {
        val timestamp = Timestamp.now()
        val normalized = TaskMapper.normalizeRequest(request, timestamp, registry)
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
                backoff_multiplier = normalized.backoffMultiplier
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

        taskSpecQueries.cancelTask(Timestamp.now(), id.value)

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

        val activeTasks = taskSpecQueries.selectAllActive().executeAsList()

        activeTasks.forEach { entity ->
            taskSpecQueries.cancelTask(Timestamp.now(), entity.id)
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
        return row.state.toPublicStatus()
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
        val normalized = TaskMapper.normalizeRequest(updatedRequest, timestamp, registry)

        taskSpecQueries.updateTask(
            state = TaskState.ENQUEUED,
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
            .map { entity -> entity?.state?.toPublicStatus() }
    }

    private fun recoverStuckTasks() {
        database.taskSpecQueries.resetRunningTasksToEnqueued(Timestamp.now())
    }
}
