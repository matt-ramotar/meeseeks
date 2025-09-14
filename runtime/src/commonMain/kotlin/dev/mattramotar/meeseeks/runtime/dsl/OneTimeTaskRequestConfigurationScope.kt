package dev.mattramotar.meeseeks.runtime.dsl

import dev.mattramotar.meeseeks.runtime.TaskPayload
import dev.mattramotar.meeseeks.runtime.TaskSchedule
import kotlin.time.Duration

@TaskRequestDsl
class OneTimeTaskRequestConfigurationScope<T : TaskPayload> @PublishedApi internal constructor(
    payload: T,
    initialDelay: Duration
) : TaskRequestConfigurationScope<T>(
    payload,
    TaskSchedule.OneTime(initialDelay)
)