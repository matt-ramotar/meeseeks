package dev.mattramotar.meeseeks.core.impl

import dev.mattramotar.meeseeks.core.Task
import dev.mattramotar.meeseeks.core.TaskParameters

internal expect class WorkRequestFactory {
    fun createWorkRequest(
        taskId: Long,
        task: Task,
        taskParameters: TaskParameters
    ): WorkRequest

    companion object {
        fun uniqueWorkNameFor(taskId: Long): String
        fun taskIdFrom(uniqueWorkName: String): Long
        val WORK_REQUEST_TAG: String
    }
}