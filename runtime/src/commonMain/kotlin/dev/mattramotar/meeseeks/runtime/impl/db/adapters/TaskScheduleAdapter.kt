package dev.mattramotar.meeseeks.runtime.impl.db.adapters

import app.cash.sqldelight.ColumnAdapter
import dev.mattramotar.meeseeks.runtime.TaskSchedule
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal class TaskScheduleAdapter(
    private val json: Json
) : ColumnAdapter<TaskSchedule, String> {
    override fun decode(databaseValue: String): TaskSchedule {
        return json.decodeFromString(databaseValue)
    }

    override fun encode(value: TaskSchedule): String {
        return json.encodeToString(value)
    }

}