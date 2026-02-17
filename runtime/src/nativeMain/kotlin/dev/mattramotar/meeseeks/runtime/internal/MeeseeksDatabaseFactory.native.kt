package dev.mattramotar.meeseeks.runtime.internal

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import dev.mattramotar.meeseeks.runtime.AppContext
import dev.mattramotar.meeseeks.runtime.db.MeeseeksDatabase
import dev.mattramotar.meeseeks.runtime.db.TaskLogEntity

internal actual class MeeseeksDatabaseFactory actual constructor() {
    actual fun create(
        context: AppContext,
        taskLogAdapter: TaskLogEntity.Adapter
    ): MeeseeksDatabase {
        val driver: SqlDriver = NativeSqliteDriver(
            schema = MeeseeksDatabase.Schema,
            name = "meeseeks.db"
        )
        return MeeseeksDatabase(
            driver,
            taskLogAdapter,
        )
    }
}