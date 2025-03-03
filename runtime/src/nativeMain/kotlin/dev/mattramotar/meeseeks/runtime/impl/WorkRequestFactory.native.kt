package dev.mattramotar.meeseeks.runtime.impl

import dev.mattramotar.meeseeks.runtime.Task

internal actual class WorkRequestFactory {
    actual fun createWorkRequest(
        taskId: Long,
        task: Task
    ): WorkRequest {
        TODO("Coming soon")
    }

    actual companion object {
        actual fun uniqueWorkNameFor(taskId: Long): String {
            TODO("Coming soon")
        }

        actual fun taskIdFrom(uniqueWorkName: String): Long {
            TODO("Coming soon")
        }

        actual val WORK_REQUEST_TAG: String
            get() = TODO("Coming soon")
    }

}