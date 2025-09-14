package dev.mattramotar.meeseeks.runtime.internal

import dev.mattramotar.meeseeks.runtime.BGTaskManagerConfig
import dev.mattramotar.meeseeks.runtime.TaskRequest
import dev.mattramotar.meeseeks.runtime.TaskSchedule

internal expect class WorkRequestFactory {
    fun createWorkRequest(
        taskId: Long,
        taskRequest: TaskRequest,
        config: BGTaskManagerConfig
    ): WorkRequest

    companion object {
        fun uniqueWorkNameFor(taskId: Long, taskSchedule: TaskSchedule): String
        fun taskIdFrom(uniqueWorkName: String, taskSchedule: TaskSchedule): Long
        val WORK_REQUEST_TAG: String
    }
}