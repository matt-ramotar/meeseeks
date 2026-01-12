package dev.mattramotar.meeseeks.runtime.internal

import dev.mattramotar.meeseeks.runtime.TaskPayload
import dev.mattramotar.meeseeks.runtime.TaskSchedule
import dev.mattramotar.meeseeks.runtime.WorkerFactory
import dev.mattramotar.meeseeks.runtime.db.TaskSpec
import dev.mattramotar.meeseeks.runtime.internal.db.model.BackoffPolicy
import dev.mattramotar.meeseeks.runtime.internal.db.model.TaskState
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalSerializationApi::class)
class TaskReschedulerTest {

    @Serializable
    private data class TestPayload(val value: String) : TaskPayload

    private val json = Json

    private fun registry(): WorkerRegistry {
        val serializer = TestPayload.serializer()
        val registration = WorkerRegistration(
            type = TestPayload::class,
            typeId = serializer.descriptor.serialName,
            serializer = serializer,
            factory = WorkerFactory<TestPayload> { error("unused") }
        )
        return WorkerRegistry(
            registrations = mapOf(TestPayload::class to registration),
            json = json
        )
    }

    private fun taskSpec(
        registry: WorkerRegistry,
        scheduleType: String,
        initialDelayMs: Long,
        intervalMs: Long,
        flexMs: Long,
        nextRunTimeMs: Long
    ): TaskSpec {
        val serialized = registry.serializePayload(TestPayload("value"))
        return TaskSpec(
            id = "task-id",
            state = TaskState.ENQUEUED,
            created_at_ms = 0L,
            updated_at_ms = 0L,
            run_attempt_count = 0L,
            platform_id = null,
            payload_type_id = serialized.typeId,
            payload_data = serialized.data,
            priority = 0L,
            requires_network = false,
            requires_charging = false,
            requires_battery_not_low = false,
            schedule_type = scheduleType,
            next_run_time_ms = nextRunTimeMs,
            initial_delay_ms = initialDelayMs,
            interval_duration_ms = intervalMs,
            flex_duration_ms = flexMs,
            backoff_policy = BackoffPolicy.LINEAR,
            backoff_delay_ms = 1000L,
            max_retries = 3L,
            backoff_multiplier = null,
            backoff_jitter_factor = 0.0
        )
    }

    @Test
    fun rescheduleRequestUsesNextRunTimeDelayForPeriodicTasks() {
        val registry = registry()
        val spec = taskSpec(
            registry = registry,
            scheduleType = "PERIODIC",
            initialDelayMs = 0L,
            intervalMs = 60_000L,
            flexMs = 15_000L,
            nextRunTimeMs = 90_000L
        )

        val request = rescheduleRequest(spec, registry, nowMs = 50_000L)
        val schedule = request.schedule as TaskSchedule.Periodic

        assertEquals(40_000L, schedule.initialDelay.inWholeMilliseconds)
        assertEquals(60_000L, schedule.interval.inWholeMilliseconds)
        assertEquals(15_000L, schedule.flexWindow.inWholeMilliseconds)
    }

    @Test
    fun rescheduleRequestClampsNegativeDelayToZero() {
        val registry = registry()
        val spec = taskSpec(
            registry = registry,
            scheduleType = "ONE_TIME",
            initialDelayMs = 10_000L,
            intervalMs = 0L,
            flexMs = 0L,
            nextRunTimeMs = 40_000L
        )

        val request = rescheduleRequest(spec, registry, nowMs = 50_000L)
        val schedule = request.schedule as TaskSchedule.OneTime

        assertEquals(0L, schedule.initialDelay.inWholeMilliseconds)
    }
}
