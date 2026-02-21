package dev.mattramotar.meeseeks.runtime.internal.db.adapters

import app.cash.sqldelight.ColumnAdapter
import dev.mattramotar.meeseeks.runtime.TaskResult
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal class TaskResultAdapter(
    private val json: Json
) : ColumnAdapter<TaskResult.Type, String> {
    override fun decode(databaseValue: String): TaskResult.Type {
        val decodedValue = try {
            json.decodeFromString<String>(databaseValue)
        } catch (_: IllegalArgumentException) {
            // Fallback for legacy enum values stored without JSON quotes.
            databaseValue
        }

        return try {
            TaskResult.Type.valueOf(decodedValue)
        } catch (error: IllegalArgumentException) {
            throw IllegalArgumentException("Unknown TaskResult.Type value: $databaseValue", error)
        }
    }

    override fun encode(value: TaskResult.Type): String {
        return json.encodeToString(value.name)
    }

}