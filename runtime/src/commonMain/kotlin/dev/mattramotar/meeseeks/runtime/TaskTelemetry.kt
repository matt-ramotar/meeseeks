package dev.mattramotar.meeseeks.runtime


fun interface TaskTelemetry {
    suspend fun onEvent(event: TaskTelemetryEvent)
}

