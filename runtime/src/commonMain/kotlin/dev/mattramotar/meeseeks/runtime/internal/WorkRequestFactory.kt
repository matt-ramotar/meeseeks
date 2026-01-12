package dev.mattramotar.meeseeks.runtime.internal

import dev.mattramotar.meeseeks.runtime.BGTaskManagerConfig
import dev.mattramotar.meeseeks.runtime.TaskRequest
import dev.mattramotar.meeseeks.runtime.TaskSchedule

internal expect class WorkRequestFactory {
    fun createWorkRequest(
        taskId: String,
        taskRequest: TaskRequest,
        config: BGTaskManagerConfig
    ): WorkRequest

    companion object {
        fun uniqueWorkNameFor(taskId: String, taskSchedule: TaskSchedule): String
        fun taskIdFrom(uniqueWorkName: String, taskSchedule: TaskSchedule): String
        val WORK_REQUEST_TAG: String
    }
}
