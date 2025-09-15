package dev.mattramotar.meeseeks.sample.shared

import dev.mattramotar.meeseeks.runtime.TaskPayload
import kotlinx.serialization.Serializable

@Serializable
data object RefreshPayload : TaskPayload {
    val stableId: String = V_1_0_0
}

// TODO: How do we handle migrations?
private const val V_1_0_0 = "dev.mattramotar.meeseeks.sample.shared.RefreshPayload:1.0.0"