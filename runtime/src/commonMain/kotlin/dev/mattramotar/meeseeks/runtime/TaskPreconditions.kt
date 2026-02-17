package dev.mattramotar.meeseeks.runtime

import kotlinx.serialization.Serializable

/**
 * Preconditions for executing a [TaskRequest].
 */
@Serializable
data class TaskPreconditions(
    val requiresNetwork: Boolean = false,
    val requiresCharging: Boolean = false,
    val requiresBatteryNotLow: Boolean = false
)