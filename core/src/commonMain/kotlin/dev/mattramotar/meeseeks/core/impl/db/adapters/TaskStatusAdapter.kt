package dev.mattramotar.meeseeks.core.impl.db.adapters

import app.cash.sqldelight.ColumnAdapter
import dev.mattramotar.meeseeks.core.TaskStatus
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal class TaskStatusAdapter(
    private val json: Json
) : ColumnAdapter<TaskStatus, String> {
    override fun decode(databaseValue: String): TaskStatus {
        return json.decodeFromString(databaseValue)
    }

    override fun encode(value: TaskStatus): String {
        return json.encodeToString(value)
    }

}