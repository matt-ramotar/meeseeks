package dev.mattramotar.meeseeks.runtime.internal

import dev.mattramotar.meeseeks.runtime.AppContext
import dev.mattramotar.meeseeks.runtime.db.MeeseeksDatabase
import dev.mattramotar.meeseeks.runtime.db.TaskLogEntity

internal expect class MeeseeksDatabaseFactory() {
    fun create(
        context: AppContext,
        taskLogAdapter: TaskLogEntity.Adapter,
    ): MeeseeksDatabase
}