package dev.mattramotar.meeseeks.runtime

public data class ScheduledTask(
    public val id: TaskId,
    public val status: TaskStatus,
    public val task: TaskRequest,
    public val runAttemptCount: Int,
    public val createdAt: Long,
    public val updatedAt: Long,
)
