package dev.mattramotar.meeseeks.runtime.impl.db.adapters

import app.cash.sqldelight.ColumnAdapter
import dev.mattramotar.meeseeks.runtime.TaskRetryPolicy
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal class TaskRetryPolicyAdapter(
    private val json: Json
) : ColumnAdapter<TaskRetryPolicy, String> {
    override fun decode(databaseValue: String): TaskRetryPolicy {
        return json.decodeFromString(databaseValue)
    }

    override fun encode(value: TaskRetryPolicy): String {
        return json.encodeToString(value)
    }

}