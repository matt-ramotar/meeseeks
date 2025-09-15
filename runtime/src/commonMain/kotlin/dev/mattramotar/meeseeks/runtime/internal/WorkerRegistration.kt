package dev.mattramotar.meeseeks.runtime.internal

import dev.mattramotar.meeseeks.runtime.TaskPayload
import dev.mattramotar.meeseeks.runtime.WorkerFactory
import kotlinx.serialization.KSerializer
import kotlin.reflect.KClass

/**
 * @param typeId Stable identifier for persistence
 */
@PublishedApi
internal data class WorkerRegistration(
    val type: KClass<out TaskPayload>,
    val typeId: String,
    val serializer: KSerializer<out TaskPayload>,
    val factory: WorkerFactory<*>
)