package dev.mattramotar.meeseeks.runtime

import kotlinx.serialization.Serializable
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Defines whether a [TaskRequest] runs once or periodically.
 */
@Serializable
public sealed class TaskSchedule {
    /**
     * Indicates the task runs once.
     */
    @Serializable
    public data class OneTime(
        public val initialDelay: Duration = 0.seconds,
    ) : TaskSchedule()

    /**
     * Indicates the task runs periodically at the specified [interval].
     */
    @Serializable
    public data class Periodic(
        public val initialDelay: Duration = 0.seconds,
        public val interval: Duration = 60.seconds,
        public val flexWindow: Duration = Duration.ZERO,
    ) : TaskSchedule()

}
