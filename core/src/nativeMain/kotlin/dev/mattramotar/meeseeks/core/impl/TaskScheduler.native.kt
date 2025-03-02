package dev.mattramotar.meeseeks.core.impl

import dev.mattramotar.meeseeks.core.Task

internal actual class TaskScheduler {
    actual fun scheduleTask(
        taskId: Long,
        task: Task,
        workRequest: WorkRequest,
        existingWorkPolicy: ExistingWorkPolicy
    ) {
    }

    actual fun isScheduled(taskId: Long): Boolean {
        TODO("Not yet implemented")
    }

    actual fun cancelWorkById(schedulerId: String) {
    }

    actual fun cancelUniqueWork(uniqueWorkName: String) {
    }

    actual fun cancelAllWorkByTag(tag: String) {
    }

}