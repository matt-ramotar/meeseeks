package dev.mattramotar.meeseeks.runtime.telemetry

import dev.mattramotar.meeseeks.runtime.TaskId
import dev.mattramotar.meeseeks.runtime.TaskPayload
import dev.mattramotar.meeseeks.runtime.TaskRequest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals

class TelemetryEventStructuredTest {

    private object TestPayload : TaskPayload

    @Test
    fun taskScheduledStructuredBuildsJsonWithoutAnySerializer() {
        val event = TelemetryEvent.TaskScheduled(
            taskId = TaskId("task-1"),
            task = TaskRequest(payload = TestPayload)
        )

        val json = Json.parseToJsonElement(event.structured()).jsonObject

        assertEquals("TASK_SCHEDULED", json.getValue("event").jsonPrimitive.content)
        assertEquals("task-1", json.getValue("taskId").jsonPrimitive.content)
        assertEquals("TestPayload", json.getValue("taskType").jsonPrimitive.content)
        assertEquals(false, json.getValue("requiresNetwork").jsonPrimitive.content.toBooleanStrict())
        assertEquals(false, json.getValue("requiresCharging").jsonPrimitive.content.toBooleanStrict())
    }

    @Test
    fun taskStatisticsStructuredSerializesNestedErrorCategoryCounts() {
        val event = TelemetryEvent.TaskStatistics(
            taskId = TaskId("task-2"),
            totalAttempts = 4,
            successfulAttempts = 3,
            failedAttempts = 1,
            transientFailures = 1,
            permanentFailures = 0,
            averageRetryDelayMs = 1250L,
            errorCategoryCounts = mapOf(
                "TRANSIENT_NETWORK" to 1
            )
        )

        val json = Json.parseToJsonElement(event.structured()).jsonObject
        val errorCategoryCounts = json.getValue("errorCategoryCounts").jsonObject

        assertEquals("TASK_STATISTICS", json.getValue("event").jsonPrimitive.content)
        assertEquals("task-2", json.getValue("taskId").jsonPrimitive.content)
        assertEquals(1, errorCategoryCounts.getValue("TRANSIENT_NETWORK").jsonPrimitive.content.toInt())
    }
}
