package dev.mattramotar.meeseeks.runtime


sealed class MeeseeksTelemetryEvent {

    data class TaskScheduled(
        val taskId: MrMeeseeksId,
        val task: Task
    ) : MeeseeksTelemetryEvent()

    data class TaskStarted(
        val taskId: MrMeeseeksId,
        val task: Task,
        val runAttemptCount: Int = 1,
    ) : MeeseeksTelemetryEvent()

    data class TaskSucceeded(
        val taskId: MrMeeseeksId,
        val task: Task,
        val runAttemptCount: Int = 1,
    ) : MeeseeksTelemetryEvent()

    data class TaskFailed(
        val taskId: MrMeeseeksId,
        val task: Task,
        val error: Throwable?,
        val runAttemptCount: Int = 1,
    ) : MeeseeksTelemetryEvent()

    data class TaskCancelled(
        val taskId: MrMeeseeksId,
        val task: Task,
        val runAttemptCount: Int = 1,
    ) : MeeseeksTelemetryEvent()
}