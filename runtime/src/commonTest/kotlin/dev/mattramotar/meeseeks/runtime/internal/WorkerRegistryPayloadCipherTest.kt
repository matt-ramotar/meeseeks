package dev.mattramotar.meeseeks.runtime.internal

import dev.mattramotar.meeseeks.runtime.PayloadCipher
import dev.mattramotar.meeseeks.runtime.TaskPayload
import dev.mattramotar.meeseeks.runtime.WorkerFactory
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@OptIn(ExperimentalSerializationApi::class)
class WorkerRegistryPayloadCipherTest {

    @Serializable
    private data class TestPayload(val value: String) : TaskPayload

    private class ReversingCipher : PayloadCipher {
        override fun encrypt(plaintext: String): String = plaintext.reversed()
        override fun decrypt(ciphertext: String): String = ciphertext.reversed()
    }

    private val json = Json

    private fun registry(cipher: PayloadCipher?): WorkerRegistry {
        val serializer = TestPayload.serializer()
        val registration = WorkerRegistration(
            type = TestPayload::class,
            typeId = serializer.descriptor.serialName,
            serializer = serializer,
            factory = WorkerFactory<TestPayload> { error("unused") }
        )

        return WorkerRegistry(
            registrations = mapOf(TestPayload::class to registration),
            json = json,
            payloadCipher = cipher
        )
    }

    @Test
    fun encryptsAndDecryptsPayloadWhenCipherConfigured() {
        val payload = TestPayload("secret")
        val registry = registry(ReversingCipher())

        val serialized = registry.serializePayload(payload)

        assertTrue(serialized.data.startsWith("enc:v1:"))
        val roundTrip = registry.deserializePayload(serialized.typeId, serialized.data)
        assertEquals(payload, roundTrip)
    }

    @Test
    fun readsPlaintextPayloadWhenCipherConfigured() {
        val payload = TestPayload("plaintext")
        val registry = registry(ReversingCipher())
        val plainData = json.encodeToString(TestPayload.serializer(), payload)

        val roundTrip = registry.deserializePayload(TestPayload.serializer().descriptor.serialName, plainData)

        assertEquals(payload, roundTrip)
    }

    @Test
    fun throwsWhenEncryptedPayloadWithoutCipher() {
        val registry = registry(null)

        val exception = assertFailsWith<TaskPayloadDeserializationException> {
            registry.deserializePayload(TestPayload.serializer().descriptor.serialName, "enc:v1:ciphertext")
        }
        assertTrue(exception.cause is IllegalStateException)
    }
}
