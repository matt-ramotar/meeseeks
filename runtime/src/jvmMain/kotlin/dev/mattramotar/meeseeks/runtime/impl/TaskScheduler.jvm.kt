package dev.mattramotar.meeseeks.runtime.impl

import dev.mattramotar.meeseeks.runtime.MeeseeksRegistry
import dev.mattramotar.meeseeks.runtime.Task
import dev.mattramotar.meeseeks.runtime.TaskSchedule
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
        TODO("Coming soon")
    }

    actual fun isScheduled(taskId: Long, taskSchedule: TaskSchedule): Boolean {
        TODO("Coming soon")
    }


    actual fun cancelWorkById(schedulerId: String, taskSchedule: TaskSchedule) {
        TODO("Coming soon")
    }

    actual fun cancelUniqueWork(uniqueWorkName: String, taskSchedule: TaskSchedule) {
        TODO("Coming soon")
    }

    actual fun cancelAllWorkByTag(tag: String) {
        TODO("Coming soon")
    }
}