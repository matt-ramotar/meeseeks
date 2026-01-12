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

        actual fun uniqueWorkNameFor(taskId: String, taskSchedule: TaskSchedule): String {
            return "$INTERNAL_PREFIX$taskId"
        }

        actual fun taskIdFrom(uniqueWorkName: String, taskSchedule: TaskSchedule): String {
            return uniqueWorkName.removePrefix(INTERNAL_PREFIX)
        }

        actual val WORK_REQUEST_TAG: String = "meeseeks"

        private const val INTERNAL_PREFIX = "meeseeks_internal_id_"
    }

}
