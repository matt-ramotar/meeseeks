package dev.mattramotar.meeseeks.runtime

import dev.mattramotar.meeseeks.runtime.dsl.TaskRequestBuilder
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

data class TaskRequest(
    val payload: TaskPayload,
    val preconditions: TaskPreconditions = TaskPreconditions(),
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val schedule: TaskSchedule = TaskSchedule.OneTime(),
    val retryPolicy: TaskRetryPolicy = TaskRetryPolicy.ExponentialBackoff(
        initialInterval = 30.seconds,
        maxRetries = 5
    )
) {
    companion object Companion {
        fun <T : TaskPayload> oneTime(payload: T, block: TaskRequestBuilder<T>.() -> Unit = {}): TaskRequest {
            return builder(payload, TaskSchedule.OneTime(), block)
        }

        fun <T : TaskPayload> periodic(
            payload: T,
            interval: Duration,
            block: TaskRequestBuilder<T>.() -> Unit = {}
        ): TaskRequest {
            return builder(payload, TaskSchedule.Periodic(interval), block)
        }

        private fun <T : TaskPayload> builder(
            payload: T,
            schedule: TaskSchedule,
            block: TaskRequestBuilder<T>.() -> Unit
        ): TaskRequest {
            return TaskRequestBuilder(payload, schedule).apply(block).build()
        }
    }
}