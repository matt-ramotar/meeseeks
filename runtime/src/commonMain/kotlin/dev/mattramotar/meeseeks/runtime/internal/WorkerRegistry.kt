package dev.mattramotar.meeseeks.runtime.internal

import dev.mattramotar.meeseeks.runtime.AppContext
import dev.mattramotar.meeseeks.runtime.PayloadCipher
import dev.mattramotar.meeseeks.runtime.TaskPayload
import dev.mattramotar.meeseeks.runtime.Worker
import dev.mattramotar.meeseeks.runtime.WorkerFactory
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlin.reflect.KClass

internal class WorkerRegistry(
    private val registrations: Map<KClass<out TaskPayload>, WorkerRegistration>,
    private val json: Json,
    private val payloadCipher: PayloadCipher? = null
) {

    private companion object {
        const val ENCRYPTION_PREFIX = "enc:v1:"
    }

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
        val storedData = encryptPayload(data)
        return SerializedPayload(registration.typeId, storedData)
    }

    fun deserializePayload(typeId: String, data: String): TaskPayload {
        val registration = registrationsByTypeId[typeId]
            ?: throw IllegalArgumentException("Unknown payload type id: $typeId. Cannot deserialize task. Ensure worker is registered.")

        @Suppress("UNCHECKED_CAST")
        val serializer = registration.serializer as KSerializer<TaskPayload>
        val decrypted = decryptPayload(data)
        return json.decodeFromString(serializer, decrypted)
    }

    internal fun getAllRegistrations(): Collection<WorkerRegistration> = registrations.values

    private fun encryptPayload(data: String): String {
        val cipher = payloadCipher ?: return data
        return ENCRYPTION_PREFIX + cipher.encrypt(data)
    }

    private fun decryptPayload(data: String): String {
        if (!data.startsWith(ENCRYPTION_PREFIX)) {
            return data
        }

        val cipher = payloadCipher
            ?: throw IllegalStateException(
                "Encrypted payload data found but no PayloadCipher configured. Configure one via ConfigurationScope.payloadCipher()."
            )
        return cipher.decrypt(data.removePrefix(ENCRYPTION_PREFIX))
    }
}
