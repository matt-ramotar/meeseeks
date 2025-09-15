package dev.mattramotar.meeseeks.sample.shared

import dev.mattramotar.meeseeks.runtime.telemetry.TelemetryEvent

interface UserLogger {
    fun log(event: TelemetryEvent)
}