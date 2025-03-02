package dev.mattramotar.meeseeks.core

import kotlinx.serialization.Serializable
import kotlin.time.Duration.Companion.seconds

/**
 * Task definition.
 */
@Serializable
data class Task(
    val meeseeksType: String,
    val preconditions: TaskPreconditions = TaskPreconditions(),
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val schedule: TaskSchedule = TaskSchedule.OneTime(),
    val retryPolicy: TaskRetryPolicy = TaskRetryPolicy.ExponentialBackoff(
        initialInterval = 30.seconds,
        maxRetries = 5
    )
)