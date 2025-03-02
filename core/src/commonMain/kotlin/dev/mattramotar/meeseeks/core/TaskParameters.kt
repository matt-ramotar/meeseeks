package dev.mattramotar.meeseeks.core

import dev.mattramotar.meeseeks.core.types.Value
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

/**
 * Information that [MrMeeseeks] needs to execute the requested [Task].
 */
@Serializable
@JvmInline
value class TaskParameters(
    val values: Map<String, Value> = emptyMap()
)



