package dev.mattramotar.meeseeks.runtime.internal.db.model

import dev.mattramotar.meeseeks.runtime.TaskStatus

enum class TaskState {
    ENQUEUED,
    RUNNING,
    SUCCEEDED,
    FAILED,
    CANCELLED;
}

internal fun TaskState.toPublicStatus(): TaskStatus = when (this) {
    TaskState.ENQUEUED -> TaskStatus.Pending
    TaskState.RUNNING -> TaskStatus.Running
    TaskState.SUCCEEDED -> TaskStatus.Finished.Completed
    TaskState.FAILED -> TaskStatus.Finished.Failed
    TaskState.CANCELLED -> TaskStatus.Finished.Cancelled
}