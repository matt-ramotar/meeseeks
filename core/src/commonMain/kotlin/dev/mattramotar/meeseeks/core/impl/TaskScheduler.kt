package dev.mattramotar.meeseeks.core.impl

import dev.mattramotar.meeseeks.core.Task


internal expect class TaskScheduler {
    fun scheduleTask(
        taskId: Long,
        task: Task,
        workRequest: WorkRequest,
        existingWorkPolicy: ExistingWorkPolicy
    )

    fun isScheduled(taskId: Long): Boolean

    fun cancelWorkById(schedulerId: String)
    fun cancelUniqueWork(uniqueWorkName: String)
    fun cancelAllWorkByTag(tag: String)
}

