package dev.mattramotar.meeseeks.runtime

import kotlinx.serialization.Serializable

/**
 * Preconditions for executing a [TaskRequest].
 */
@Serializable
public data class TaskPreconditions(
    public val requiresNetwork: Boolean = false,
    public val requiresCharging: Boolean = false,
    public val requiresBatteryNotLow: Boolean = false,
)
