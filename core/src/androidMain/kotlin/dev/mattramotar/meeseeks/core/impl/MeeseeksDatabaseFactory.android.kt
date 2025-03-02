package dev.mattramotar.meeseeks.core.impl

import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import dev.mattramotar.meeseeks.core.MeeseeksContext
import dev.mattramotar.meeseeks.core.db.MeeseeksDatabase
import dev.mattramotar.meeseeks.core.db.TaskEntity
import dev.mattramotar.meeseeks.core.db.TaskLogEntity

internal actual class MeeseeksDatabaseFactory actual constructor() {
    actual fun create(
        context: MeeseeksContext,
        taskAdapter: TaskEntity.Adapter,
        taskLogAdapter: TaskLogEntity.Adapter
    ): MeeseeksDatabase {
        val driver = AndroidSqliteDriver(
            schema = MeeseeksDatabase.Schema,
            context = context.applicationContext,
            name = "meeseeks.db"
        )

        return MeeseeksDatabase.Companion(
            driver,
            taskAdapter,
            taskLogAdapter
        )
    }
}