package dev.mattramotar.meeseeks.runtime


sealed class TaskTelemetryEvent {

    data class TaskScheduled(
        val taskId: TaskId,
        val task: Task
    ) : TaskTelemetryEvent()

    data class TaskStarted(
        val taskId: TaskId,
        val task: Task,
        val runAttemptCount: Int = 1,
    ) : TaskTelemetryEvent()

    data class TaskSucceeded(
        val taskId: TaskId,
        val task: Task,
        val runAttemptCount: Int = 1,
    ) : TaskTelemetryEvent()

    data class TaskFailed(
        val taskId: TaskId,
        val task: Task,
        val error: Throwable?,
        val runAttemptCount: Int = 1,
    ) : TaskTelemetryEvent()

    data class TaskCancelled(
        val taskId: TaskId,
        val task: Task,
        val runAttemptCount: Int = 1,
    ) : TaskTelemetryEvent()
}