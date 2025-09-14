package dev.mattramotar.meeseeks.sample.shared

import dev.mattramotar.meeseeks.runtime.TaskTelemetryEvent

interface UserLogger {
    fun log(event: TaskTelemetryEvent)
}