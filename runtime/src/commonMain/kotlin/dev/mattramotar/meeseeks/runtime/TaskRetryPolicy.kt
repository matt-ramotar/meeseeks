package dev.mattramotar.meeseeks.runtime

import kotlinx.serialization.Serializable
import kotlin.time.Duration

/**
 * Retry strategy.
 */
@Serializable
sealed class TaskRetryPolicy {
    /**
     * Retries on a fixed interval until [maxRetries] is reached or [TaskWorker] succeeds.
     */
    @Serializable
    data class FixedInterval(
        val retryInterval: Duration,
        val maxRetries: Int? = null
    ) : TaskRetryPolicy()

    /**
     * Retries with an exponentially increasing delay until [maxRetries] is reached or [TaskWorker] succeeds.
     */
    @Serializable
    data class ExponentialBackoff(
        val initialInterval: Duration,
        val maxRetries: Int,
        val multiplier: Double = 2.0,
        val maxInterval: Duration? = null,
        val jitterFactor: Double = 0.1
    ) : TaskRetryPolicy()
}