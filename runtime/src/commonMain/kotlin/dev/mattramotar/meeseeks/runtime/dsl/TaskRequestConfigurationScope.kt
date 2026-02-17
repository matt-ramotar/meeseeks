package dev.mattramotar.meeseeks.runtime.dsl

import dev.mattramotar.meeseeks.runtime.TaskPayload
import dev.mattramotar.meeseeks.runtime.TaskPreconditions
import dev.mattramotar.meeseeks.runtime.TaskPriority
import dev.mattramotar.meeseeks.runtime.TaskRequest
import dev.mattramotar.meeseeks.runtime.TaskRetryPolicy
import dev.mattramotar.meeseeks.runtime.TaskSchedule
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@TaskRequestDsl
public open class TaskRequestConfigurationScope<T : TaskPayload> internal constructor(
    private val payload: T,
    private var schedule: TaskSchedule,
) {
    private var preconditions: TaskPreconditions = TaskPreconditions()
    private var priority: TaskPriority = TaskPriority.MEDIUM
    private var retryPolicy: TaskRetryPolicy = TaskRetryPolicy.ExponentialBackoff(
        initialInterval = 30.seconds,
        maxRetries = 5
    )

    public fun requireNetwork(required: Boolean = true) {
        preconditions = preconditions.copy(requiresNetwork = required)
    }

    public fun requireCharging(required: Boolean = true) {
        preconditions = preconditions.copy(requiresCharging = required)
    }

    public fun requireBatteryNotLow(required: Boolean = true) {
        preconditions = preconditions.copy(requiresBatteryNotLow = required)
    }

    public fun lowPriority() {
        priority = TaskPriority.LOW
    }

    public fun highPriority() {
        priority = TaskPriority.HIGH
    }

    public fun mediumPriority() {
        priority = TaskPriority.MEDIUM
    }

    public fun retryWithExponentialBackoff(
        initialDelay: Duration = 30.seconds,
        maxAttempts: Int = 5,
    ) {
        require(maxAttempts > 0) { "maxAttempts must be positive." }
        retryPolicy = TaskRetryPolicy.ExponentialBackoff(initialDelay, maxAttempts)
    }

    @PublishedApi
    internal fun build(): TaskRequest {
        return TaskRequest(
            payload = payload,
            preconditions = preconditions,
            priority = priority,
            schedule = schedule,
            retryPolicy = retryPolicy,
        )
    }
}
