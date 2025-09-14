package dev.mattramotar.meeseeks.runtime

import kotlinx.serialization.Serializable
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Defines whether a [LegacyTask] runs once or periodically.
 */
@Serializable
sealed class TaskSchedule {
    /**
     * Indicates the task runs once.
     */
    @Serializable
    data class OneTime(
        val initialDelay: Duration = 0.seconds
    ) : TaskSchedule()

    /**
     * Indicates the task runs periodically at the specified [interval].
     */
    @Serializable
    data class Periodic(
        val initialDelay: Duration = 0.seconds,
        val interval: Duration = 60.seconds,
        val flexWindow: Duration = Duration.ZERO
    ) : TaskSchedule()

}

