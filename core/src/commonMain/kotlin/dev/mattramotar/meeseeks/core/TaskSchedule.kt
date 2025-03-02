package dev.mattramotar.meeseeks.core

import kotlinx.serialization.Serializable
import kotlin.time.Duration

/**
 * Defines whether a [Task] runs once or periodically.
 */
@Serializable
sealed class TaskSchedule {
    /**
     * Indicates the task runs once.
     */
    @Serializable
    data class OneTime(
        val initialDelay: Long = 0L
    ) : TaskSchedule()

    /**
     * Indicates the task runs periodically at the specified [interval].
     */
    @Serializable
    data class Periodic(
        val interval: Duration,
        val flexWindow: Duration = Duration.ZERO
    ) : TaskSchedule()

}

