package dev.mattramotar.meeseeks.sample.demo

import dev.mattramotar.meeseeks.runtime.telemetry.StructuredTelemetry
import dev.mattramotar.meeseeks.runtime.telemetry.Telemetry
import dev.mattramotar.meeseeks.runtime.telemetry.TelemetryEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

internal class DemoTelemetry(
    private val maxEventRetention: Int,
) : Telemetry {

    private val structuredTelemetry = StructuredTelemetry(
        StructuredTelemetry.Config(
            enableStatistics = true,
            enableStructuredLogs = true,
            maxStatsRetention = 1000,
        )
    )

    private val _events = MutableStateFlow<List<String>>(emptyList())
    val events: StateFlow<List<String>> = _events.asStateFlow()

    override suspend fun onEvent(event: TelemetryEvent) {
        structuredTelemetry.onEvent(event)
        val payload = event.structured()
        _events.update { current ->
            (current + payload).takeLast(maxEventRetention)
        }
    }

    fun snapshot(): DemoTelemetrySnapshot {
        val recentEvents = events.value
        return DemoTelemetrySnapshot(
            totalEvents = recentEvents.size,
            recentEvents = recentEvents,
            aggregatedStatistics = structuredTelemetry.getAggregatedStatistics().mapValues { (_, value) ->
                value?.toString() ?: "null"
            },
            statisticsJson = structuredTelemetry.exportStatisticsAsJson(),
            eventsJson = structuredTelemetry.exportEventsAsJson(),
        )
    }

    fun clear() {
        structuredTelemetry.clearAll()
        _events.value = emptyList()
    }
}
