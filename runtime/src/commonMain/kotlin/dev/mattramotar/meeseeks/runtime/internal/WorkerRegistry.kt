package dev.mattramotar.meeseeks.runtime.internal

import dev.mattramotar.meeseeks.runtime.AppContext
import dev.mattramotar.meeseeks.runtime.TaskPayload
import dev.mattramotar.meeseeks.runtime.Worker
import dev.mattramotar.meeseeks.runtime.WorkerFactory
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlin.reflect.KClass

internal class WorkerRegistry(
    private val registrations: Map<KClass<out TaskPayload>, WorkerRegistration>,
    private val json: Json
) {

    private val registrationsByTypeId = registrations.values.associateBy { it.typeId }

    fun <T : TaskPayload> getFactory(payloadClass: KClass<T>): WorkerFactory<T> {
        val registration = registrations[payloadClass]

        @Suppress("UNCHECKED_CAST")
        return registration?.factory as? WorkerFactory<T>
            ?: throw IllegalArgumentException("No Worker registered for Payload type: ${payloadClass.simpleName}. Ensure it was added in BGTaskManagerBuilder.")
    }

    inline fun <reified T : TaskPayload> getWorker(appContext: AppContext): Worker<T> {
        val factory = getFactory(T::class)
        return factory.create(appContext)
    }

    fun serializePayload(payload: TaskPayload): SerializedPayload {
        val registration = registrations[payload::class] ?: throw IllegalArgumentException("Payload type not registered: ${payload::class.simpleName}.")

        @Suppress("UNCHECKED_CAST")
        val serializer = registration.serializer as KSerializer<TaskPayload>
        val data = json.encodeToString(serializer, payload)
        return SerializedPayload(registration.typeId, data)
    }

    fun deserializePayload(typeId: String, data: String): TaskPayload {
        val registration = registrationsByTypeId[typeId]
            ?: throw IllegalArgumentException("Unknown payload type id: $typeId. Cannot deserialize task. Ensure worker is registered.")

        @Suppress("UNCHECKED_CAST")
        val serializer = registration.serializer as KSerializer<TaskPayload>
        return json.decodeFromString(serializer, data)
    }

    internal fun getAllRegistrations(): Collection<WorkerRegistration> = registrations.values
}