package dev.mattramotar.meeseeks.runtime.telemetry


public fun interface Telemetry {
    public suspend fun onEvent(event: TelemetryEvent)
}
