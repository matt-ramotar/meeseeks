package dev.mattramotar.meeseeks.runtime.impl

import dev.mattramotar.meeseeks.runtime.Task
import dev.mattramotar.meeseeks.runtime.TaskSchedule

internal expect class WorkRequestFactory {
    fun createWorkRequest(
        taskId: Long,
        task: Task
    ): WorkRequest

    companion object {
        fun uniqueWorkNameFor(taskId: Long, taskSchedule: TaskSchedule): String
        fun taskIdFrom(uniqueWorkName: String, taskSchedule: TaskSchedule): Long
        val WORK_REQUEST_TAG: String
    }
}