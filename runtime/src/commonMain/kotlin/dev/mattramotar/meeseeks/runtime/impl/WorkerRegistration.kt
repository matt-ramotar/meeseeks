package dev.mattramotar.meeseeks.runtime.impl

import dev.mattramotar.meeseeks.runtime.TaskPayload
import dev.mattramotar.meeseeks.runtime.WorkerFactory
import kotlinx.serialization.KSerializer
import kotlin.reflect.KClass

@PublishedApi
internal data class WorkerRegistration(
    val type: KClass<out TaskPayload>,
    val serializer: KSerializer<out TaskPayload>,
    val factory: WorkerFactory<*>
)