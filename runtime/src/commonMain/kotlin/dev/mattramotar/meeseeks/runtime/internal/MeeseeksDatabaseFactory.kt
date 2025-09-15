package dev.mattramotar.meeseeks.runtime.internal

import dev.mattramotar.meeseeks.runtime.AppContext
import dev.mattramotar.meeseeks.runtime.db.MeeseeksDatabase
import dev.mattramotar.meeseeks.runtime.db.TaskLogEntity
import dev.mattramotar.meeseeks.runtime.db.TaskSpec

internal expect class MeeseeksDatabaseFactory() {
    fun create(
        context: AppContext,
        taskSpecAdapter: TaskSpec.Adapter,
        taskLogAdapter: TaskLogEntity.Adapter,
    ): MeeseeksDatabase
}