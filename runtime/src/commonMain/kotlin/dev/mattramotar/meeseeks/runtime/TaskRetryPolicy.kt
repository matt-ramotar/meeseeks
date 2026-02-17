package dev.mattramotar.meeseeks.runtime

import kotlinx.serialization.Serializable
import kotlin.time.Duration

/**
 * Retry strategy.
 */
@Serializable
public sealed class TaskRetryPolicy {
    /**
     * Retries on a fixed interval until [maxRetries] is reached or [Worker] succeeds.
     */
    @Serializable
    public data class FixedInterval(
        public val retryInterval: Duration,
        public val maxRetries: Int? = null,
    ) : TaskRetryPolicy()

    /**
     * Retries with an exponentially increasing delay until [maxRetries] is reached or [Worker] succeeds.
     */
    @Serializable
    public data class ExponentialBackoff(
        public val initialInterval: Duration,
        public val maxRetries: Int,
        public val multiplier: Double = 2.0,
        public val maxInterval: Duration? = null,
        public val jitterFactor: Double = 0.1,
    ) : TaskRetryPolicy()
}
