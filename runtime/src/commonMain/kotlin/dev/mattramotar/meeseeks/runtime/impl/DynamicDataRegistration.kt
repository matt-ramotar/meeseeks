package dev.mattramotar.meeseeks.runtime.impl

import dev.mattramotar.meeseeks.runtime.DynamicData
import dev.mattramotar.meeseeks.runtime.WorkerFactory
import kotlinx.serialization.KSerializer
import kotlin.reflect.KClass

@PublishedApi
internal data class DynamicDataRegistration(
    val type: KClass<out DynamicData>,
    val serializer: KSerializer<out DynamicData>,
    val factory: WorkerFactory<*>
)