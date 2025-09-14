package dev.mattramotar.meeseeks.runtime.internal

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOneOrNull
import dev.mattramotar.meeseeks.runtime.BGTaskManager
import dev.mattramotar.meeseeks.runtime.BGTaskManagerConfig
import dev.mattramotar.meeseeks.runtime.ScheduledTask
import dev.mattramotar.meeseeks.runtime.TaskId
import dev.mattramotar.meeseeks.runtime.TaskRequest
import dev.mattramotar.meeseeks.runtime.TaskStatus
import dev.mattramotar.meeseeks.runtime.TaskTelemetry
import dev.mattramotar.meeseeks.runtime.TaskTelemetryEvent
import dev.mattramotar.meeseeks.runtime.db.MeeseeksDatabase
import dev.mattramotar.meeseeks.runtime.db.TaskEntity
import dev.mattramotar.meeseeks.runtime.internal.coroutines.MeeseeksDispatchers
import dev.mattramotar.meeseeks.runtime.internal.extensions.TaskEntityExtensions.toTaskRequest
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
    private val config: BGTaskManagerConfig,
    override val coroutineContext: CoroutineContext = SupervisorJob() + MeeseeksDispatchers.IO,
    private val telemetry: TaskTelemetry? = null,
) : BGTaskManager, CoroutineScope {


    init {
        launch {
            taskRescheduler.rescheduleTasks()
        }
    }

    override fun schedule(request: TaskRequest): TaskId {
        val taskQueries = database.taskQueries
        val timestamp = Timestamp.now()

        taskQueries.insertTask(
            payload = request.payload,
            preconditions = request.preconditions,
            priority = request.priority,
            schedule = request.schedule,
            retryPolicy = request.retryPolicy,
            status = TaskStatus.Pending,
            workRequestId = null,
            createdAt = timestamp,
            updatedAt = timestamp
        )

        val taskId = taskQueries.lastInsertedTaskId().executeAsOne()
        val workRequest = workRequestFactory.createWorkRequest(taskId, request, config)

        taskScheduler.scheduleTask(taskId, request, workRequest, ExistingWorkPolicy.KEEP)

        taskQueries.updateWorkRequestId(
            workRequestId = workRequest.id,
            updatedAt = Timestamp.now(),
            id = taskId
        )

        launch {
            telemetry?.onEvent(
                TaskTelemetryEvent.TaskScheduled(
                    taskId = TaskId(taskId),
                    task = request
                )
            )
        }

        return TaskId(taskId)
    }

    override fun cancel(id: TaskId) {
        val taskQueries = database.taskQueries
        val taskEntity =
            taskQueries.selectTaskByTaskId(id.value).executeAsOneOrNull() ?: return

        val workRequestId = taskEntity.workRequestId
        if (!workRequestId.isNullOrEmpty()) {
            taskScheduler.cancelWorkById(workRequestId, taskEntity.schedule)
        } else {
            taskScheduler.cancelUniqueWork(
                uniqueWorkName = WorkRequestFactory.uniqueWorkNameFor(
                    taskEntity.id,
                    taskEntity.schedule
                ),
                taskEntity.schedule
            )
        }

        taskQueries.cancelTask(Timestamp.now(), id.value)

        launch {
            telemetry?.onEvent(
                TaskTelemetryEvent.TaskCancelled(
                    taskId = id,
                    task = taskEntity.toTaskRequest()
                )
            )
        }
    }

    override fun cancelAll() {
        taskScheduler.cancelAllWorkByTag(WorkRequestFactory.WORK_REQUEST_TAG)

        val taskQueries = database.taskQueries

        val activeTasks = taskQueries.selectAllActive().executeAsList()

        activeTasks.forEach { entity ->
            taskQueries.cancelTask(Timestamp.now(), entity.id)
            launch {
                telemetry?.onEvent(
                    TaskTelemetryEvent.TaskCancelled(
                        taskId = TaskId(entity.id),
                        task = entity.toTaskRequest()
                    )
                )
            }
        }
    }

    override fun reschedulePendingTasks() {
        val activeTasks = database.taskQueries.selectAllActive().executeAsList()
        activeTasks.forEach { taskEntity ->
            if (taskEntity.workRequestId.isNullOrBlank()) {
                taskRescheduler.rescheduleTask(taskEntity)
            }
        }
    }

    override fun getTaskStatus(id: TaskId): TaskStatus? {
        val row = database.taskQueries
            .selectTaskByTaskId(id.value)
            .executeAsOneOrNull() ?: return null
        return row.status
    }

    override fun listTasks(): List<ScheduledTask> {
        return database.taskQueries.selectAllTasks()
            .executeAsList()
            .map { it.toScheduledTask() }
    }


    override fun reschedule(
        id: TaskId,
        updatedRequest: TaskRequest
    ): TaskId {
        val taskQueries = database.taskQueries
        val existing = taskQueries.selectTaskByTaskId(id.value).executeAsOneOrNull()
            ?: error("Update failed: Task $id not found.")

        val existingWorkRequestId = existing.workRequestId
        if (!existingWorkRequestId.isNullOrEmpty()) {
            taskScheduler.cancelWorkById(existingWorkRequestId, existing.schedule)
        } else {
            taskScheduler.cancelUniqueWork(
                uniqueWorkName = WorkRequestFactory.uniqueWorkNameFor(
                    existing.id,
                    existing.schedule
                ),
                existing.schedule
            )
        }

        val timestamp = Timestamp.now()
        taskQueries.updateTask(
            payload = updatedRequest.payload,
            preconditions = updatedRequest.preconditions,
            priority = updatedRequest.priority,
            schedule = updatedRequest.schedule,
            retryPolicy = updatedRequest.retryPolicy,
            status = TaskStatus.Pending,
            updatedAt = timestamp,
            id = existing.id
        )

        val newWorkRequest = workRequestFactory.createWorkRequest(existing.id, updatedRequest, config)
        taskScheduler.scheduleTask(
            existing.id,
            updatedRequest,
            newWorkRequest,
            ExistingWorkPolicy.KEEP
        )

        taskQueries.updateWorkRequestId(newWorkRequest.id, timestamp, existing.id)

        launch {
            telemetry?.onEvent(
                TaskTelemetryEvent.TaskScheduled(
                    taskId = TaskId(existing.id),
                    task = updatedRequest
                )
            )
        }

        return id
    }

    override fun observeStatus(id: TaskId): Flow<TaskStatus?> {
        return database.taskQueries
            .selectTaskByTaskId(id.value)
            .asFlow()
            .mapToOneOrNull(context = MeeseeksDispatchers.IO)
            .map { entity -> entity?.status }
    }

    private fun TaskEntity.toScheduledTask() = ScheduledTask(
        id = TaskId(this.id),
        status = this.status,
        task = this.toTaskRequest(),
        runAttemptCount = this.runAttemptCount.toInt(),
        createdAt = this.createdAt,
        updatedAt = this.updatedAt
    )
}