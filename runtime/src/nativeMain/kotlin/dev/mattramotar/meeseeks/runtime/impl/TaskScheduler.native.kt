package dev.mattramotar.meeseeks.runtime.impl

import dev.mattramotar.meeseeks.runtime.Task

internal actual class TaskScheduler {
    actual fun scheduleTask(
        taskId: Long,
        task: Task,
        workRequest: WorkRequest,
        existingWorkPolicy: ExistingWorkPolicy
    ) {
    }

    actual fun isScheduled(taskId: Long): Boolean {
        TODO("Coming soon")
    }

    actual fun cancelWorkById(schedulerId: String) {
    }

    actual fun cancelUniqueWork(uniqueWorkName: String) {
    }

    actual fun cancelAllWorkByTag(tag: String) {
    }

}