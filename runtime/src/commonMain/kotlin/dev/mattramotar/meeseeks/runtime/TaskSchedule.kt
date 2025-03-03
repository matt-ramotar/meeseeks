package dev.mattramotar.meeseeks.runtime

import kotlinx.serialization.Serializable
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

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
        val interval: Duration = 60.seconds,
        val flexWindow: Duration = Duration.ZERO
    ) : TaskSchedule()

}

