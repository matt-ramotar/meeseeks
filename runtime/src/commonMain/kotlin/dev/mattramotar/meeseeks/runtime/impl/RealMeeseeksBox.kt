package dev.mattramotar.meeseeks.runtime.impl

import dev.mattramotar.meeseeks.runtime.MeeseeksBox
import dev.mattramotar.meeseeks.runtime.MeeseeksBoxConfig
import dev.mattramotar.meeseeks.runtime.MeeseeksTelemetry
import dev.mattramotar.meeseeks.runtime.MeeseeksTelemetryEvent
import dev.mattramotar.meeseeks.runtime.MrMeeseeksId
import dev.mattramotar.meeseeks.runtime.Task
import dev.mattramotar.meeseeks.runtime.TaskStatus
import dev.mattramotar.meeseeks.runtime.db.MeeseeksDatabase
import dev.mattramotar.meeseeks.runtime.impl.coroutines.MeeseeksDispatchers
import dev.mattramotar.meeseeks.runtime.impl.extensions.TaskEntityExtensions.toTask
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext


internal class RealMeeseeksBox(
    private val database: MeeseeksDatabase,
    private val workRequestFactory: WorkRequestFactory,
    private val taskScheduler: TaskScheduler,
    private val taskRescheduler: TaskRescheduler,
    override val coroutineContext: CoroutineContext = SupervisorJob() + MeeseeksDispatchers.IO,
    private val telemetry: MeeseeksTelemetry? = null,
) : MeeseeksBox, CoroutineScope {


    init {
        launch {
            taskRescheduler.rescheduleTasks()
        }
    }

    override fun summon(task: Task): MrMeeseeksId {
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
                MeeseeksTelemetryEvent.TaskScheduled(
                    taskId = MrMeeseeksId(taskId),
                    task = task
                )
            )
        }

        return MrMeeseeksId(taskId)
    }

    override fun sendBackToBox(id: MrMeeseeksId) {
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
                MeeseeksTelemetryEvent.TaskCancelled(
                    taskId = id,
                    task = taskEntity.toTask()
                )
            )
        }
    }

    override fun sendAllBackToBox() {
        taskScheduler.cancelAllWorkByTag(WorkRequestFactory.WORK_REQUEST_TAG)

        val taskQueries = database.taskQueries

        val activeTasks = taskQueries.selectAllActive().executeAsList()

        activeTasks.forEach { entity ->
            taskQueries.cancelTask(Timestamp.now(), entity.id)
            launch {
                telemetry?.onEvent(
                    MeeseeksTelemetryEvent.TaskCancelled(
                        taskId = MrMeeseeksId(entity.id),
                        task = entity.toTask()
                    )
                )
            }
        }
    }

    override fun triggerCheckForDueTasks() {
        val activeTasks = database.taskQueries.selectAllActive().executeAsList()
        activeTasks.forEach { taskEntity ->
            if (taskEntity.workRequestId.isNullOrBlank()) {
                taskRescheduler.rescheduleTask(taskEntity)
            }
        }
    }
}