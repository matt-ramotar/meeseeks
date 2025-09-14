package dev.mattramotar.meeseeks.runtime

import dev.mattramotar.meeseeks.runtime.dsl.TaskRequestBuilder
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

data class TaskRequest(
    val data: DynamicData,
    val preconditions: TaskPreconditions = TaskPreconditions(),
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val schedule: TaskSchedule = TaskSchedule.OneTime(),
    val retryPolicy: TaskRetryPolicy = TaskRetryPolicy.ExponentialBackoff(
        initialInterval = 30.seconds,
        maxRetries = 5
    )
) {
    companion object Companion {
        fun <T : DynamicData> oneTime(task: T, block: TaskRequestBuilder<T>.() -> Unit = {}): TaskRequest {
            return builder(task, TaskSchedule.OneTime(), block)
        }

        fun <T : DynamicData> periodic(
            data: T,
            interval: Duration,
            block: TaskRequestBuilder<T>.() -> Unit = {}
        ): TaskRequest {
            return builder(data, TaskSchedule.Periodic(interval), block)
        }

        private fun <T : DynamicData> builder(
            data: T,
            schedule: TaskSchedule,
            block: TaskRequestBuilder<T>.() -> Unit
        ): TaskRequest {
            return TaskRequestBuilder(data, schedule).apply(block).build()
        }
    }
}