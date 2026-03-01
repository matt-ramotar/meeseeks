package dev.mattramotar.meeseeks.sample.demo

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

public data class DemoScheduleRequest(
    public val scenario: DemoScenario,
    public val periodic: Boolean = scenario.defaultPeriodic,
    public val interval: Duration = 30.seconds,
    public val initialDelay: Duration = Duration.ZERO,
    public val requiresNetwork: Boolean = false,
    public val requiresCharging: Boolean = false,
    public val requiresBatteryNotLow: Boolean = false,
    public val highPriority: Boolean = false,
    public val maxAttempts: Int = 3,
    public val backoffInitialDelay: Duration = 5.seconds,
)
