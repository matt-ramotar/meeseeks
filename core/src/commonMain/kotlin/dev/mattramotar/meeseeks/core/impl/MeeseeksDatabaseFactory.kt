package dev.mattramotar.meeseeks.core.impl

import dev.mattramotar.meeseeks.core.MeeseeksContext
import dev.mattramotar.meeseeks.core.db.MeeseeksDatabase
import dev.mattramotar.meeseeks.core.db.TaskEntity
import dev.mattramotar.meeseeks.core.db.TaskLogEntity

internal expect class MeeseeksDatabaseFactory() {
    fun create(
        context: MeeseeksContext,
        taskAdapter: TaskEntity.Adapter,
        taskLogAdapter: TaskLogEntity.Adapter
    ): MeeseeksDatabase
}