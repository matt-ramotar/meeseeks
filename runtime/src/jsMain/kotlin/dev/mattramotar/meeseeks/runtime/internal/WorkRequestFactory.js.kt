package dev.mattramotar.meeseeks.runtime.internal

import dev.mattramotar.meeseeks.runtime.BGTaskManagerConfig
import dev.mattramotar.meeseeks.runtime.TaskRequest
import dev.mattramotar.meeseeks.runtime.TaskSchedule

internal actual class WorkRequestFactory {
    actual fun createWorkRequest(
        taskId: String,
        taskRequest: TaskRequest,
        config: BGTaskManagerConfig
    ): WorkRequest {
        return WorkRequest(taskId)
    }

    actual companion object {
        private const val UNIQUE_WORK_NAME_PREFIX = "meeseeks_work_"

        actual fun uniqueWorkNameFor(
            taskId: String,
            taskSchedule: TaskSchedule
        ): String = UNIQUE_WORK_NAME_PREFIX + taskId

        actual fun taskIdFrom(
            uniqueWorkName: String,
            taskSchedule: TaskSchedule
        ): String = uniqueWorkName.removePrefix(UNIQUE_WORK_NAME_PREFIX)

        actual val WORK_REQUEST_TAG: String = "meeseeks"

        fun taskIdFrom(tag: String): String {
            return tag.removePrefix("$WORK_REQUEST_TAG-")
        }

        fun createTag(taskId: String): String {
            return "$WORK_REQUEST_TAG-$taskId"
        }
    }

}
