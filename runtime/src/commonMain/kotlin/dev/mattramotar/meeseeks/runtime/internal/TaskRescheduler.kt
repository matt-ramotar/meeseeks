package dev.mattramotar.meeseeks.runtime.internal

import dev.mattramotar.meeseeks.runtime.BGTaskManagerConfig
import dev.mattramotar.meeseeks.runtime.TaskResult
import dev.mattramotar.meeseeks.runtime.db.MeeseeksDatabase
import dev.mattramotar.meeseeks.runtime.db.TaskEntity
import dev.mattramotar.meeseeks.runtime.internal.extensions.TaskEntityExtensions.toTaskRequest
import kotlinx.datetime.Clock


internal interface TaskRescheduler {
    fun rescheduleTasks()
    fun rescheduleTask(taskEntity: TaskEntity)

    companion object {
        operator fun invoke(
            database: MeeseeksDatabase,
            taskScheduler: TaskScheduler,
            workRequestFactory: WorkRequestFactory,
            config: BGTaskManagerConfig
        ): TaskRescheduler {
            return RealTaskRescheduler(database, taskScheduler, workRequestFactory, config)
        }
    }
}

private class RealTaskRescheduler(
    private val database: MeeseeksDatabase,
    private val taskScheduler: TaskScheduler,
    private val workRequestFactory: WorkRequestFactory,
    private val config: BGTaskManagerConfig
) : TaskRescheduler {

    override fun rescheduleTasks() {
        val taskEntities = database.taskQueries.selectAllPending().executeAsList()
        for (taskEntity in taskEntities) {
            if (!taskScheduler.isScheduled(taskEntity.id, taskEntity.schedule)) {
                rescheduleTask(taskEntity)
            }
        }
    }

    override fun rescheduleTask(taskEntity: TaskEntity) {
        val taskRequest = taskEntity.toTaskRequest()
        val workRequest =
            workRequestFactory.createWorkRequest(taskEntity.id, taskRequest, config)

        taskScheduler.scheduleTask(taskEntity.id, taskRequest, workRequest, ExistingWorkPolicy.REPLACE)

        val now = Clock.System.now().toEpochMilliseconds()
        database.taskQueries.updateWorkRequestId(
            workRequestId = workRequest.id,
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