package dev.mattramotar.meeseeks.runtime.impl.db.adapters

import app.cash.sqldelight.ColumnAdapter
import dev.mattramotar.meeseeks.runtime.TaskResult
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal class TaskResultAdapter(
    private val json: Json
) : ColumnAdapter<TaskResult.Type, String> {
    override fun decode(databaseValue: String): TaskResult.Type {
        return json.decodeFromString(databaseValue)
    }

    override fun encode(value: TaskResult.Type): String {
        return json.encodeToString(value)
    }

}