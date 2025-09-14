package dev.mattramotar.meeseeks.runtime.dsl

import dev.mattramotar.meeseeks.runtime.TaskPayload
import dev.mattramotar.meeseeks.runtime.TaskSchedule
import kotlin.time.Duration

@TaskRequestDsl
class PeriodicTaskRequestConfigurationScope<T : TaskPayload> @PublishedApi internal constructor(
    payload: T,
    initialDelay: Duration,
    interval: Duration,
    flexWindow: Duration,
) : TaskRequestConfigurationScope<T>(
    payload,
    TaskSchedule.Periodic(initialDelay, interval, flexWindow)
)