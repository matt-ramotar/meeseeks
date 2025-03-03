package dev.mattramotar.meeseeks.core.impl

import dev.mattramotar.meeseeks.core.Task

internal actual class WorkRequestFactory {
    actual fun createWorkRequest(
        taskId: Long,
        task: Task
    ): WorkRequest = WorkRequest(taskId)

    actual companion object {
        private const val UNIQUE_WORK_NAME_PREFIX = "meeseeks_work_"

        actual fun uniqueWorkNameFor(taskId: Long): String {
            return UNIQUE_WORK_NAME_PREFIX + taskId
        }

        actual fun taskIdFrom(uniqueWorkName: String): Long {
            return uniqueWorkName.removePrefix(UNIQUE_WORK_NAME_PREFIX).toLong()
        }

        actual val WORK_REQUEST_TAG: String = "meeseeks"
    }

}