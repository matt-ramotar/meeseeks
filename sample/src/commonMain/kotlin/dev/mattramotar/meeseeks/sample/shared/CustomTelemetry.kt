package dev.mattramotar.meeseeks.sample.shared

import dev.mattramotar.meeseeks.runtime.telemetry.Telemetry
import dev.mattramotar.meeseeks.runtime.telemetry.TelemetryEvent

class CustomTelemetry(private val logger: UserLogger) : Telemetry {
    override suspend fun onEvent(event: TelemetryEvent) {
        logger.log(event)
    }
}