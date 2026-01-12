package dev.mattramotar.meeseeks.runtime.internal

import dev.mattramotar.meeseeks.runtime.BGTaskManagerConfig
import dev.mattramotar.meeseeks.runtime.TaskPayload
import dev.mattramotar.meeseeks.runtime.TaskRequest
import dev.mattramotar.meeseeks.runtime.TaskRetryPolicy
import dev.mattramotar.meeseeks.runtime.WorkerFactory
import dev.mattramotar.meeseeks.runtime.internal.db.TaskMapper
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalSerializationApi::class)
class TaskMapperNormalizationTest {

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

    private fun normalize(
        request: TaskRequest,
        config: BGTaskManagerConfig
    ): TaskMapper.NormalizedRequest {
        return TaskMapper.normalizeRequest(
            request = request,
            currentTimeMs = 0L,
            registry = registry(),
            config = config
        )
    }

    @Test
    fun exponentialBackoffCappedByConfigMaxRetryCount() {
        val request = TaskRequest(
            payload = TestPayload("value"),
            retryPolicy = TaskRetryPolicy.ExponentialBackoff(
                initialInterval = 1.seconds,
                maxRetries = 10
            )
        )

        val normalized = normalize(request, BGTaskManagerConfig(maxRetryCount = 3))

        assertEquals(3L, normalized.maxRetries)
    }

    @Test
    fun fixedIntervalUsesConfigMaxRetryCountWhenNull() {
        val request = TaskRequest(
            payload = TestPayload("value"),
            retryPolicy = TaskRetryPolicy.FixedInterval(
                retryInterval = 1.seconds,
                maxRetries = null
            )
        )

        val normalized = normalize(request, BGTaskManagerConfig(maxRetryCount = 4))

        assertEquals(4L, normalized.maxRetries)
    }

    @Test
    fun fixedIntervalDoesNotIncreaseBelowConfigMaxRetryCount() {
        val request = TaskRequest(
            payload = TestPayload("value"),
            retryPolicy = TaskRetryPolicy.FixedInterval(
                retryInterval = 1.seconds,
                maxRetries = 2
            )
        )

        val normalized = normalize(request, BGTaskManagerConfig(maxRetryCount = 5))

        assertEquals(2L, normalized.maxRetries)
    }
}
