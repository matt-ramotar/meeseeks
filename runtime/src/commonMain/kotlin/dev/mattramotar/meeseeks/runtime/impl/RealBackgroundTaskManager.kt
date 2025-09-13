package dev.mattramotar.meeseeks.runtime.impl

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOneOrNull
import dev.mattramotar.meeseeks.runtime.BackgroundTaskManager
import dev.mattramotar.meeseeks.runtime.TaskTelemetry
import dev.mattramotar.meeseeks.runtime.TaskTelemetryEvent
import dev.mattramotar.meeseeks.runtime.TaskId
import dev.mattramotar.meeseeks.runtime.ScheduledTask
import dev.mattramotar.meeseeks.runtime.Task
import dev.mattramotar.meeseeks.runtime.TaskStatus
import dev.mattramotar.meeseeks.runtime.db.MeeseeksDatabase
import dev.mattramotar.meeseeks.runtime.db.TaskEntity
import dev.mattramotar.meeseeks.runtime.impl.coroutines.MeeseeksDispatchers
import dev.mattramotar.meeseeks.runtime.impl.extensions.TaskEntityExtensions.toTask
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext


internal class RealBackgroundTaskManager(
    private val database: MeeseeksDatabase,
    private val workRequestFactory: WorkRequestFactory,
    private val taskScheduler: TaskScheduler,
    private val taskRescheduler: TaskRescheduler,
    override val coroutineContext: CoroutineContext = SupervisorJob() + MeeseeksDispatchers.IO,
    private val telemetry: TaskTelemetry? = null,
) : BackgroundTaskManager, CoroutineScope {


    init {
        launch {
            taskRescheduler.rescheduleTasks()
        }
    }

    override fun enqueue(task: Task): TaskId {
        val taskQueries = database.taskQueries
        val timestamp = Timestamp.now()

        taskQueries.insertTask(
            meeseeksType = task.meeseeksType,
            preconditions = task.preconditions,
            priority = task.priority,
            schedule = task.schedule,
            retryPolicy = task.retryPolicy,
            status = TaskStatus.Pending,
            parameters = task.parameters,
            workRequestId = null,
            createdAt = timestamp,
            updatedAt = timestamp
        )

        val taskId = taskQueries.lastInsertedTaskId().executeAsOne()
        val workRequest = workRequestFactory.createWorkRequest(taskId, task)

        taskScheduler.scheduleTask(taskId, task, workRequest, ExistingWorkPolicy.KEEP)

        taskQueries.updateWorkRequestId(
            workRequestId = workRequest.id,
            updatedAt = Timestamp.now(),
            id = taskId
        )

        launch {
            telemetry?.onEvent(
                TaskTelemetryEvent.TaskScheduled(
                    taskId = TaskId(taskId),
                    task = task
                )
            )
        }

        return TaskId(taskId)
    }

    override fun cancel(id: TaskId) {
        val taskQueries = database.taskQueries
        val taskEntity =
            taskQueries.selectTaskByMrMeeseeksId(id.value).executeAsOneOrNull() ?: return

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
                    task = taskEntity.toTask()
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
                        task = entity.toTask()
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
            .selectTaskByMrMeeseeksId(id.value)
            .executeAsOneOrNull() ?: return null
        return row.status
    }

    override fun getAllTasks(): List<ScheduledTask> {
        return database.taskQueries.selectAllTasks()
            .executeAsList()
            .map { it.toScheduledTask() }
    }

    override fun updateTask(id: TaskId, newTask: Task): TaskId {
        val taskQueries = database.taskQueries
        val existing = taskQueries.selectTaskByMrMeeseeksId(id.value).executeAsOneOrNull()
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
            meeseeksType = newTask.meeseeksType,
            preconditions = newTask.preconditions,
            priority = newTask.priority,
            schedule = newTask.schedule,
            retryPolicy = newTask.retryPolicy,
            status = TaskStatus.Pending,
            parameters = newTask.parameters,
            updatedAt = timestamp,
            id = existing.id
        )

        val newWorkRequest = workRequestFactory.createWorkRequest(existing.id, newTask)
        taskScheduler.scheduleTask(
            existing.id,
            newTask,
            newWorkRequest,
            ExistingWorkPolicy.KEEP
        )

        taskQueries.updateWorkRequestId(newWorkRequest.id, timestamp, existing.id)

        launch {
            telemetry?.onEvent(
                TaskTelemetryEvent.TaskScheduled(
                    taskId = TaskId(existing.id),
                    task = newTask
                )
            )
        }

        return id
    }

    override fun watchStatus(id: TaskId): Flow<TaskStatus?> {
        return database.taskQueries
            .selectTaskByMrMeeseeksId(id.value)
            .asFlow()
            .mapToOneOrNull(context = MeeseeksDispatchers.IO)
            .map { entity -> entity?.status }
    }

    private fun TaskEntity.toScheduledTask() = ScheduledTask(
        id = TaskId(this.id),
        status = this.status,
        task = this.toTask(),
        runAttemptCount = this.runAttemptCount.toInt(),
        createdAt = this.createdAt,
        updatedAt = this.updatedAt
    )
}