package dev.mattramotar.meeseeks.runtime


sealed class TaskTelemetryEvent {

    data class TaskScheduled(
        val taskId: TaskId,
        val task: TaskRequest
    ) : TaskTelemetryEvent()

    data class TaskStarted(
        val taskId: TaskId,
        val task: TaskRequest,
        val runAttemptCount: Int = 1,
    ) : TaskTelemetryEvent()

    data class TaskSucceeded(
        val taskId: TaskId,
        val task: TaskRequest,
        val runAttemptCount: Int = 1,
    ) : TaskTelemetryEvent()

    data class TaskFailed(
        val taskId: TaskId,
        val task: TaskRequest,
        val error: Throwable?,
        val runAttemptCount: Int = 1,
    ) : TaskTelemetryEvent()

    data class TaskCancelled(
        val taskId: TaskId,
        val task: TaskRequest,
        val runAttemptCount: Int = 1,
    ) : TaskTelemetryEvent()
}