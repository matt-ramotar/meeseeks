package dev.mattramotar.meeseeks.sample.shared

import dev.mattramotar.meeseeks.runtime.TaskTelemetry
import dev.mattramotar.meeseeks.runtime.TaskTelemetryEvent

class CustomTelemetry(private val logger: UserLogger) : TaskTelemetry {
    override suspend fun onEvent(event: TaskTelemetryEvent) {
        logger.log(event)
    }
}