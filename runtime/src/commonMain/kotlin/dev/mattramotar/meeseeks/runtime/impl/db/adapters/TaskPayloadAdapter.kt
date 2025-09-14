package dev.mattramotar.meeseeks.runtime.impl.db.adapters

import app.cash.sqldelight.ColumnAdapter
import dev.mattramotar.meeseeks.runtime.TaskPayload
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal class TaskPayloadAdapter(
    private val json: Json
) : ColumnAdapter<TaskPayload, String> {
    override fun decode(databaseValue: String): TaskPayload {
        return json.decodeFromString(databaseValue)
    }

    override fun encode(value: TaskPayload): String {
        return json.encodeToString(value)
    }
}