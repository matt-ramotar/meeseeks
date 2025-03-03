package dev.mattramotar.meeseeks.runtime.impl

import dev.mattramotar.meeseeks.runtime.Task

internal actual class WorkRequestFactory {
    actual fun createWorkRequest(
        taskId: Long,
        task: Task
    ): WorkRequest {
        TODO("Not yet implemented")
    }

    actual companion object {
        actual fun uniqueWorkNameFor(taskId: Long): String {
            TODO("Not yet implemented")
        }

        actual fun taskIdFrom(uniqueWorkName: String): Long {
            TODO("Not yet implemented")
        }

        actual val WORK_REQUEST_TAG: String
            get() = TODO("Not yet implemented")
    }

}