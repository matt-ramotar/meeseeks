package dev.mattramotar.meeseeks.runtime.impl

import dev.mattramotar.meeseeks.runtime.Task

internal expect class WorkRequestFactory {
    fun createWorkRequest(
        taskId: Long,
        task: Task
    ): WorkRequest

    companion object {
        fun uniqueWorkNameFor(taskId: Long): String
        fun taskIdFrom(uniqueWorkName: String): Long
        val WORK_REQUEST_TAG: String
    }
}