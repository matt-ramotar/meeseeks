package dev.mattramotar.meeseeks.runtime.types

import dev.mattramotar.meeseeks.runtime.internal.Timestamp

/**
 * Enhanced error types for task retry scenarios.
 * These provide detailed categorization of failures for better observability and debugging.
 */

/**
 * Base exception for all retry-related errors
 */
public abstract class TaskRetryException(
    message: String,
    cause: Throwable? = null,
    public val attemptNumber: Int = 0,
    public val maxRetries: Int = 0,
) : Exception(message, cause)

/**
 * Thrown when a task has exceeded its maximum retry attempts
 */
public class MaxRetriesExceededException(
    taskId: String,
    attemptNumber: Int,
    maxRetries: Int,
    cause: Throwable? = null,
) : TaskRetryException(
    message = "Task $taskId exceeded maximum retry attempts ($attemptNumber/$maxRetries)",
    cause = cause,
    attemptNumber = attemptNumber,
    maxRetries = maxRetries,
)

/**
 * Thrown when a task experiences a rate limit error
 */
public class RateLimitException(
    message: String = "Rate limit exceeded",
    public val retryAfterMs: Long? = null,
    attemptNumber: Int = 0,
    maxRetries: Int = 0,
) : TaskRetryException(
    message = if (retryAfterMs != null) "$message. Retry after ${retryAfterMs}ms" else message,
    attemptNumber = attemptNumber,
    maxRetries = maxRetries,
)

/**
 * Thrown when a task experiences a timeout
 */
public class TaskTimeoutException(
    message: String = "Task execution timed out",
    public val timeoutMs: Long,
    attemptNumber: Int = 0,
    maxRetries: Int = 0,
) : TaskRetryException(
    message = "$message after ${timeoutMs}ms",
    attemptNumber = attemptNumber,
    maxRetries = maxRetries,
)

/**
 * Thrown when a dependent service is unavailable
 */
public class ServiceUnavailableException(
    serviceName: String,
    cause: Throwable? = null,
    attemptNumber: Int = 0,
    maxRetries: Int = 0,
) : TaskRetryException(
    message = "Service '$serviceName' is unavailable",
    cause = cause,
    attemptNumber = attemptNumber,
    maxRetries = maxRetries,
)

/**
 * Thrown when a task cannot acquire a required resource
 */
public class ResourceUnavailableException(
    resourceName: String,
    cause: Throwable? = null,
    attemptNumber: Int = 0,
    maxRetries: Int = 0,
) : TaskRetryException(
    message = "Resource '$resourceName' is unavailable",
    cause = cause,
    attemptNumber = attemptNumber,
    maxRetries = maxRetries,
)

/**
 * Thrown when there's a conflict that might be resolved by retry
 */
public class TransientConflictException(
    message: String = "Transient conflict detected",
    cause: Throwable? = null,
    attemptNumber: Int = 0,
    maxRetries: Int = 0,
) : TaskRetryException(
    message = message,
    cause = cause,
    attemptNumber = attemptNumber,
    maxRetries = maxRetries,
)

/**
 * Thrown when circuit breaker is open for a service
 */
public class CircuitBreakerOpenException(
    serviceName: String,
    public val openUntilMs: Long? = null,
    attemptNumber: Int = 0,
    maxRetries: Int = 0,
) : TaskRetryException(
    message = if (openUntilMs != null) {
        "Circuit breaker open for '$serviceName' until $openUntilMs"
    } else {
        "Circuit breaker open for '$serviceName'"
    },
    attemptNumber = attemptNumber,
    maxRetries = maxRetries,
)

/**
 * Categories of retry errors for telemetry and monitoring
 */
public enum class RetryErrorCategory {
    NETWORK,
    RATE_LIMIT,
    TIMEOUT,
    SERVICE_UNAVAILABLE,
    RESOURCE_CONTENTION,
    CIRCUIT_BREAKER,
    MAX_RETRIES_EXCEEDED,
    UNKNOWN
}

/**
 * Categorize exceptions for retry policy decisions
 */
public object RetryErrorClassifier {

    public fun classify(error: Throwable): RetryErrorCategory {
        return when (error) {
            is MaxRetriesExceededException -> RetryErrorCategory.MAX_RETRIES_EXCEEDED
            is RateLimitException -> RetryErrorCategory.RATE_LIMIT
            is TaskTimeoutException -> RetryErrorCategory.TIMEOUT
            is ServiceUnavailableException -> RetryErrorCategory.SERVICE_UNAVAILABLE
            is ResourceUnavailableException -> RetryErrorCategory.RESOURCE_CONTENTION
            is CircuitBreakerOpenException -> RetryErrorCategory.CIRCUIT_BREAKER
            is TransientNetworkException -> RetryErrorCategory.NETWORK
            is TransientConflictException -> RetryErrorCategory.RESOURCE_CONTENTION
            else -> RetryErrorCategory.UNKNOWN
        }
    }

    public fun isRetryable(error: Throwable): Boolean {
        return when (classify(error)) {
            RetryErrorCategory.MAX_RETRIES_EXCEEDED -> false
            RetryErrorCategory.UNKNOWN -> {
                // For unknown errors, check if it's explicitly marked as permanent
                error !is PermanentValidationException
            }
            else -> true // All other categories are retryable
        }
    }

    public fun suggestedRetryDelay(error: Throwable, baseDelayMs: Long): Long {
        return when (error) {
            is RateLimitException -> error.retryAfterMs ?: baseDelayMs * 5
            is CircuitBreakerOpenException -> {
                error.openUntilMs?.let { openUntil -> openUntil - Timestamp.now() }
                    ?.coerceAtLeast(baseDelayMs) ?: (baseDelayMs * 10)
            }
            is TaskTimeoutException -> baseDelayMs * 2 // Longer delay for timeouts
            is ServiceUnavailableException -> baseDelayMs * 3
            else -> baseDelayMs
        }
    }
}
