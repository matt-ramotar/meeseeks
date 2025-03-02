package dev.mattramotar.meeseeks.core.impl.db.adapters

import app.cash.sqldelight.ColumnAdapter
import dev.mattramotar.meeseeks.core.TaskPriority
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal class TaskPriorityAdapter(
    private val json: Json
) : ColumnAdapter<TaskPriority, String> {
    override fun decode(databaseValue: String): TaskPriority {
        return json.decodeFromString(databaseValue)
    }

    override fun encode(value: TaskPriority): String {
        return json.encodeToString(value)
    }

}