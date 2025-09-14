package dev.mattramotar.meeseeks.runtime.impl

import dev.mattramotar.meeseeks.runtime.TaskRequest
import dev.mattramotar.meeseeks.runtime.TaskSchedule

internal actual class WorkRequestFactory {
    actual fun createWorkRequest(
        taskId: Long,
        taskRequest: TaskRequest,
    ): WorkRequest {
        return WorkRequest(taskId)
    }

    actual companion object {
        private const val UNIQUE_WORK_NAME_PREFIX = "meeseeks_work_"

        actual fun uniqueWorkNameFor(
            taskId: Long,
            taskSchedule: TaskSchedule
        ): String = UNIQUE_WORK_NAME_PREFIX + taskId

        actual fun taskIdFrom(
            uniqueWorkName: String,
            taskSchedule: TaskSchedule
        ): Long = uniqueWorkName.removePrefix(UNIQUE_WORK_NAME_PREFIX).toLong()

        actual val WORK_REQUEST_TAG: String = "meeseeks"

        fun taskIdFrom(tag: String): Long {
            return tag.removePrefix("$WORK_REQUEST_TAG-").toLong()
        }

        fun createTag(taskId: Long): String {
            return "$WORK_REQUEST_TAG-$taskId"
        }
    }

}
