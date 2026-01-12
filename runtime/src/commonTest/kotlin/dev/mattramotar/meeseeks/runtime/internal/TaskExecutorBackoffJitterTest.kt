package dev.mattramotar.meeseeks.runtime.internal

import dev.mattramotar.meeseeks.runtime.TaskResult
import dev.mattramotar.meeseeks.runtime.db.TaskSpec
import dev.mattramotar.meeseeks.runtime.internal.db.model.BackoffPolicy
import dev.mattramotar.meeseeks.runtime.internal.db.model.TaskState
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.minutes

class TaskExecutorBackoffJitterTest {

    private class FixedRandom(
        private val chooser: (Long, Long) -> Long
    ) : Random() {
        override fun nextBits(bitCount: Int): Int = 0

        override fun nextLong(from: Long, until: Long): Long = chooser(from, until)
    }

    private fun taskSpec(
        backoffPolicy: BackoffPolicy,
        backoffDelayMs: Long,
        backoffMultiplier: Double?,
        backoffJitterFactor: Double
    ): TaskSpec {
        return TaskSpec(
            id = "task-id",
            state = TaskState.ENQUEUED,
            created_at_ms = 0L,
            updated_at_ms = 0L,
            run_attempt_count = 0L,
            platform_id = null,
            payload_type_id = "payload",
            payload_data = "{}",
            priority = 0L,
            requires_network = false,
            requires_charging = false,
            requires_battery_not_low = false,
            schedule_type = "ONE_TIME",
            next_run_time_ms = 0L,
            initial_delay_ms = 0L,
            interval_duration_ms = 0L,
            flex_duration_ms = 0L,
            backoff_policy = backoffPolicy,
            backoff_delay_ms = backoffDelayMs,
            max_retries = 3L,
            backoff_multiplier = backoffMultiplier,
            backoff_jitter_factor = backoffJitterFactor
        )
    }

    @Test
    fun exponentialBackoffUsesJitterLowerBound() {
        val spec = taskSpec(
            backoffPolicy = BackoffPolicy.EXPONENTIAL,
            backoffDelayMs = 1000L,
            backoffMultiplier = 2.0,
            backoffJitterFactor = 0.1
        )
        val random = FixedRandom { from, _ -> from }

        val delay = TaskExecutor.calculateRetryDelay(spec, TaskResult.Retry, 1, random)

        assertEquals(900L, delay)
    }

    @Test
    fun exponentialBackoffUsesJitterUpperBound() {
        val spec = taskSpec(
            backoffPolicy = BackoffPolicy.EXPONENTIAL,
            backoffDelayMs = 1000L,
            backoffMultiplier = 2.0,
            backoffJitterFactor = 0.1
        )
        val random = FixedRandom { _, until -> until - 1 }

        val delay = TaskExecutor.calculateRetryDelay(spec, TaskResult.Retry, 1, random)

        assertEquals(1100L, delay)
    }

    @Test
    fun exponentialBackoffClampsJitterToMaxDelay() {
        val maxDelayMs = 5.minutes.inWholeMilliseconds
        val spec = taskSpec(
            backoffPolicy = BackoffPolicy.EXPONENTIAL,
            backoffDelayMs = maxDelayMs,
            backoffMultiplier = 10.0,
            backoffJitterFactor = 0.5
        )
        val random = FixedRandom { _, until -> until - 1 }

        val delay = TaskExecutor.calculateRetryDelay(spec, TaskResult.Retry, 2, random)

        assertEquals(maxDelayMs, delay)
    }

    @Test
    fun linearBackoffIgnoresJitter() {
        val spec = taskSpec(
            backoffPolicy = BackoffPolicy.LINEAR,
            backoffDelayMs = 1000L,
            backoffMultiplier = null,
            backoffJitterFactor = 1.0
        )
        val random = FixedRandom { _, until -> until - 1 }

        val delay = TaskExecutor.calculateRetryDelay(spec, TaskResult.Retry, 3, random)

        assertEquals(3000L, delay)
    }
}
