package dev.mattramotar.meeseeks.runtime.impl

import dev.mattramotar.meeseeks.runtime.MeeseeksRegistry
import dev.mattramotar.meeseeks.runtime.Task
import dev.mattramotar.meeseeks.runtime.db.MeeseeksDatabase

internal actual class TaskScheduler(
    private val database: MeeseeksDatabase,
    private val registry: MeeseeksRegistry
) {
    actual fun scheduleTask(
        taskId: Long,
        task: Task,
        workRequest: WorkRequest,
        existingWorkPolicy: ExistingWorkPolicy
    ) {
        TODO()
    }

    actual fun isScheduled(taskId: Long): Boolean {
        TODO()
    }


    actual fun cancelWorkById(schedulerId: String) {
        TODO()
    }

    actual fun cancelUniqueWork(uniqueWorkName: String) {
        TODO()
    }

    actual fun cancelAllWorkByTag(tag: String) {
        TODO()
    }
}