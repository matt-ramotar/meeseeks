package dev.mattramotar.meeseeks.runtime


interface MeeseeksTelemetry {
    suspend fun onEvent(event: MeeseeksTelemetryEvent)
}

