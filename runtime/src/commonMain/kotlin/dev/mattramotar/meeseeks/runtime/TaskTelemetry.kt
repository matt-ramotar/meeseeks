package dev.mattramotar.meeseeks.runtime


interface TaskTelemetry {
    suspend fun onEvent(event: TaskTelemetryEvent)
}

