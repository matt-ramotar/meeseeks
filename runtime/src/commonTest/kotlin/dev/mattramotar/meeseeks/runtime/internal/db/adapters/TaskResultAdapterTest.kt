package dev.mattramotar.meeseeks.runtime.internal.db.adapters

import dev.mattramotar.meeseeks.runtime.TaskResult
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class TaskResultAdapterTest {

    private val adapter = TaskResultAdapter(Json)

    @Test
    fun roundTripEncodeDecodeSupportsAllResultTypes() {
        TaskResult.Type.entries.forEach { type ->
            val encoded = adapter.encode(type)
            val decoded = adapter.decode(encoded)

            assertEquals(type, decoded)
        }
    }

    @Test
    fun decodeSupportsJsonQuotedLegacyValue() {
        val serializedLegacyValue = Json.encodeToString(TaskResult.Type.Success.name)

        val decoded = adapter.decode(serializedLegacyValue)

        assertEquals(TaskResult.Type.Success, decoded)
    }

    @Test
    fun decodeSupportsPlainTokenFallback() {
        val decoded = adapter.decode(TaskResult.Type.Success.name)

        assertEquals(TaskResult.Type.Success, decoded)
    }

    @Test
    fun decodeFailsClearlyForUnknownToken() {
        val error = assertFailsWith<IllegalArgumentException> {
            adapter.decode("UnknownValue")
        }

        assertTrue(error.message?.contains("Unknown TaskResult.Type value: UnknownValue") == true)
    }
}
