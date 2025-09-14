package dev.mattramotar.meeseeks.runtime

data class ScheduledTask(
    val id: TaskId,
    val status: TaskStatus,
    val task: TaskRequest,
    val runAttemptCount: Int,
    val createdAt: Long,
    val updatedAt: Long
)