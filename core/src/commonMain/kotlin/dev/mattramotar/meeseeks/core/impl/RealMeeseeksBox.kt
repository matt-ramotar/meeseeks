package dev.mattramotar.meeseeks.core.impl

import dev.mattramotar.meeseeks.core.MeeseeksBox
import dev.mattramotar.meeseeks.core.MrMeeseeksId
import dev.mattramotar.meeseeks.core.Task
import dev.mattramotar.meeseeks.core.TaskStatus
import dev.mattramotar.meeseeks.core.db.MeeseeksDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext


internal class RealMeeseeksBox(
    private val database: MeeseeksDatabase,
    private val workRequestFactory: WorkRequestFactory,
    private val taskScheduler: TaskScheduler,
    private val taskRescheduler: TaskRescheduler,
    override val coroutineContext: CoroutineContext = SupervisorJob() + Dispatchers.IO,
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
            workRequestId = workRequest.id.toString(),
            updatedAt = Timestamp.now(),
            id = taskId
        )

        return MrMeeseeksId(taskId)
    }

    override fun sendBackToBox(id: MrMeeseeksId) {
        val taskQueries = database.taskQueries
        val taskEntity =
            taskQueries.selectTaskByMrMeeseeksId(id.value).executeAsOneOrNull() ?: return

        val workRequestId = taskEntity.workRequestId
        if (!workRequestId.isNullOrEmpty()) {
            taskScheduler.cancelWorkById(workRequestId)
        } else {
            taskScheduler.cancelUniqueWork(
                uniqueWorkName = WorkRequestFactory.uniqueWorkNameFor(
                    taskEntity.id
                )
            )
        }

        taskQueries.cancelTask(Timestamp.now(), id.value)
    }

    override fun sendAllBackToBox() {
        taskScheduler.cancelAllWorkByTag(WorkRequestFactory.WORK_REQUEST_TAG)

        val taskQueries = database.taskQueries
        taskQueries.selectAllActive().executeAsList().forEach {
            taskQueries.cancelTask(Timestamp.now(), it.id)
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