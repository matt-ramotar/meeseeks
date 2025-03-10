package dev.mattramotar.meeseeks.runtime

data class ScheduledTask(
    val id: MrMeeseeksId,
    val status: TaskStatus,
    val task: Task,
    val runAttemptCount: Int,
    val createdAt: Long,
    val updatedAt: Long
)