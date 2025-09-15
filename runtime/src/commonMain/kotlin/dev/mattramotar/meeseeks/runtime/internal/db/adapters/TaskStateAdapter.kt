package dev.mattramotar.meeseeks.runtime.internal.db.adapters

import app.cash.sqldelight.ColumnAdapter
import dev.mattramotar.meeseeks.runtime.internal.db.model.TaskState

internal object TaskStateAdapter : ColumnAdapter<TaskState, String> {
    override fun decode(databaseValue: String): TaskState = TaskState.valueOf(databaseValue)
    override fun encode(value: TaskState): String = value.name
}