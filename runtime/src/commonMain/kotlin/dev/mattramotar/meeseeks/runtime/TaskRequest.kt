package dev.mattramotar.meeseeks.runtime

import dev.mattramotar.meeseeks.runtime.dsl.TaskRequestConfigurationScope
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

public data class TaskRequest(
    public val payload: TaskPayload,
    public val preconditions: TaskPreconditions = TaskPreconditions(),
    public val priority: TaskPriority = TaskPriority.MEDIUM,
    public val schedule: TaskSchedule = TaskSchedule.OneTime(),
    public val retryPolicy: TaskRetryPolicy = TaskRetryPolicy.ExponentialBackoff(
        initialInterval = 30.seconds,
        maxRetries = 5,
    ),
) {
    public companion object Companion {
        public fun <T : TaskPayload> oneTime(
            payload: T,
            block: TaskRequestConfigurationScope<T>.() -> Unit = {},
        ): TaskRequest {
            return builder(payload, TaskSchedule.OneTime(), block)
        }

        public fun <T : TaskPayload> periodic(
            payload: T,
            interval: Duration,
            block: TaskRequestConfigurationScope<T>.() -> Unit = {},
        ): TaskRequest {
            return builder(payload, TaskSchedule.Periodic(interval = interval), block)
        }

        private fun <T : TaskPayload> builder(
            payload: T,
            schedule: TaskSchedule,
            block: TaskRequestConfigurationScope<T>.() -> Unit
        ): TaskRequest {
            return TaskRequestConfigurationScope(payload, schedule).apply(block).build()
        }
    }
}
