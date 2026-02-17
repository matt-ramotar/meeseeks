package dev.mattramotar.meeseeks.runtime.internal.db.model

import dev.mattramotar.meeseeks.runtime.TaskStatus

internal enum class TaskState {
    ENQUEUED,
    RUNNING,
    SUCCEEDED,
    FAILED,
    CANCELLED;

    companion object {
        fun fromDbValue(value: String): TaskState = try {
            valueOf(value)
        } catch (e: IllegalArgumentException) {
            error("Unknown TaskState value: $value")
        }
    }
}

internal fun TaskState.toDbValue(): String = name

internal fun TaskState.toPublicStatus(): TaskStatus = when (this) {
    TaskState.ENQUEUED -> TaskStatus.Pending
    TaskState.RUNNING -> TaskStatus.Running
    TaskState.SUCCEEDED -> TaskStatus.Finished.Completed
    TaskState.FAILED -> TaskStatus.Finished.Failed
    TaskState.CANCELLED -> TaskStatus.Finished.Cancelled
}