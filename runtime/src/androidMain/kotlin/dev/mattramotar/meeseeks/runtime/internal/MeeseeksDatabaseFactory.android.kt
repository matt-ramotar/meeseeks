package dev.mattramotar.meeseeks.runtime.internal

import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import dev.mattramotar.meeseeks.runtime.AppContext
import dev.mattramotar.meeseeks.runtime.db.MeeseeksDatabase
import dev.mattramotar.meeseeks.runtime.db.TaskLogEntity
import dev.mattramotar.meeseeks.runtime.db.TaskSpec

internal actual class MeeseeksDatabaseFactory actual constructor() {
    actual fun create(
        context: AppContext,
        taskSpecAdapter: TaskSpec.Adapter,
        taskLogAdapter: TaskLogEntity.Adapter
    ): MeeseeksDatabase {
        val driver = AndroidSqliteDriver(
            schema = MeeseeksDatabase.Schema,
            context = context.applicationContext,
            name = "meeseeks.db"
        )

        return MeeseeksDatabase(
            driver,
            taskLogAdapter,
            taskSpecAdapter
        )
    }
}