package dev.mattramotar.meeseeks.runtime.impl.db.adapters

import app.cash.sqldelight.ColumnAdapter
import dev.mattramotar.meeseeks.runtime.TaskPreconditions
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal class TaskPreconditionsAdapter(
    private val json: Json
) : ColumnAdapter<TaskPreconditions, String> {
    override fun decode(databaseValue: String): TaskPreconditions {
        return json.decodeFromString(databaseValue)
    }

    override fun encode(value: TaskPreconditions): String {
        return json.encodeToString(value)
    }

}