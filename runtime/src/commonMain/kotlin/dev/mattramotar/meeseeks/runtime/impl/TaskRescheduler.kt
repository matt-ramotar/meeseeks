package dev.mattramotar.meeseeks.runtime.impl

import dev.mattramotar.meeseeks.runtime.TaskResult
import dev.mattramotar.meeseeks.runtime.db.MeeseeksDatabase
import dev.mattramotar.meeseeks.runtime.db.TaskEntity
import dev.mattramotar.meeseeks.runtime.impl.extensions.TaskEntityExtensions.toTask
import kotlinx.datetime.Clock


internal interface TaskRescheduler {
    fun rescheduleTasks()
    fun rescheduleTask(taskEntity: TaskEntity)

    companion object {
        operator fun invoke(
            database: MeeseeksDatabase,
            taskScheduler: TaskScheduler,
            workRequestFactory: WorkRequestFactory
        ): TaskRescheduler {
            return RealTaskRescheduler(database, taskScheduler, workRequestFactory)
        }
    }
}

private class RealTaskRescheduler(
    private val database: MeeseeksDatabase,
    private val taskScheduler: TaskScheduler,
    private val workRequestFactory: WorkRequestFactory
) : TaskRescheduler {

    override fun rescheduleTasks() {
        val taskEntities = database.taskQueries.selectAllPending().executeAsList()
        for (taskEntity in taskEntities) {
            if (!taskScheduler.isScheduled(taskEntity.id)) {
                rescheduleTask(taskEntity)
            }
        }
    }

    override fun rescheduleTask(taskEntity: TaskEntity) {
        val task = taskEntity.toTask()
        val workRequest =
            workRequestFactory.createWorkRequest(taskEntity.id, task)

        taskScheduler.scheduleTask(taskEntity.id, task, workRequest, ExistingWorkPolicy.REPLACE)

        val now = Clock.System.now().toEpochMilliseconds()
        database.taskQueries.updateWorkRequestId(
            workRequestId = workRequest.id.toString(),
            updatedAt = now,
            id = taskEntity.id
        )

        database.taskLogQueries.insertLog(
            taskId = taskEntity.id,
            created = now,
            result = TaskResult.Retry.type,
            attempt = taskEntity.runAttemptCount,
            message = "Task resurrected by TaskRescheduler."
        )
    }
}