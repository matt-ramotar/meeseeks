package dev.mattramotar.meeseeks.runtime.internal

import dev.mattramotar.meeseeks.runtime.TaskRequest
import dev.mattramotar.meeseeks.runtime.TaskSchedule


internal expect class TaskScheduler {
    fun scheduleTask(
        taskId: Long,
        task: TaskRequest,
        workRequest: WorkRequest,
        existingWorkPolicy: ExistingWorkPolicy
    )

    fun isScheduled(taskId: Long, taskSchedule: TaskSchedule): Boolean

    fun cancelWorkById(schedulerId: String, taskSchedule: TaskSchedule)
    fun cancelUniqueWork(uniqueWorkName: String, taskSchedule: TaskSchedule)
    fun cancelAllWorkByTag(tag: String)
}

