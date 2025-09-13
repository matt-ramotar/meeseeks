package dev.mattramotar.meeseeks.runtime

import dev.mattramotar.meeseeks.runtime.types.Value
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

/**
 * Information that [TaskWorker] needs to execute the requested [Task].
 */
@Serializable
@JvmInline
value class TaskParameters(
    val values: Map<String, Value> = emptyMap()
)



