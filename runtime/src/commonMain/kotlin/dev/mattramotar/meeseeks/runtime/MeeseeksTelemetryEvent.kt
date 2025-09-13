package dev.mattramotar.meeseeks.runtime


sealed class MeeseeksTelemetryEvent {

    data class TaskScheduled(
        val taskId: TaskId,
        val task: Task
    ) : MeeseeksTelemetryEvent()

    data class TaskStarted(
        val taskId: TaskId,
        val task: Task,
        val runAttemptCount: Int = 1,
    ) : MeeseeksTelemetryEvent()

    data class TaskSucceeded(
        val taskId: TaskId,
        val task: Task,
        val runAttemptCount: Int = 1,
    ) : MeeseeksTelemetryEvent()

    data class TaskFailed(
        val taskId: TaskId,
        val task: Task,
        val error: Throwable?,
        val runAttemptCount: Int = 1,
    ) : MeeseeksTelemetryEvent()

    data class TaskCancelled(
        val taskId: TaskId,
        val task: Task,
        val runAttemptCount: Int = 1,
    ) : MeeseeksTelemetryEvent()
}