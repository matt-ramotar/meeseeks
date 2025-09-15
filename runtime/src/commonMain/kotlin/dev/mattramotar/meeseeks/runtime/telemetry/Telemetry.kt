package dev.mattramotar.meeseeks.runtime.telemetry


fun interface Telemetry {
    suspend fun onEvent(event: TelemetryEvent)
}

