package dev.mattramotar.meeseeks.core.impl.db.adapters

import app.cash.sqldelight.ColumnAdapter
import dev.mattramotar.meeseeks.core.TaskParameters
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal class TaskParametersAdapter(
    private val json: Json
) : ColumnAdapter<TaskParameters, String> {
    override fun decode(databaseValue: String): TaskParameters {
        return json.decodeFromString(databaseValue)
    }

    override fun encode(value: TaskParameters): String {
        return json.encodeToString(value)
    }

}