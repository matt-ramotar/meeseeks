package dev.mattramotar.meeseeks.runtime.impl

import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import dev.mattramotar.meeseeks.runtime.AppContext
import dev.mattramotar.meeseeks.runtime.db.MeeseeksDatabase
import dev.mattramotar.meeseeks.runtime.db.TaskEntity
import dev.mattramotar.meeseeks.runtime.db.TaskLogEntity

internal actual class MeeseeksDatabaseFactory actual constructor() {
    actual fun create(
        context: AppContext,
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