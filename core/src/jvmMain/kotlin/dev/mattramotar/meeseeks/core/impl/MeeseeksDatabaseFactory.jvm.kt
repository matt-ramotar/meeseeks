package dev.mattramotar.meeseeks.core.impl

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
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
        val driver = JdbcSqliteDriver("jdbc:sqlite:meeseeks.db", schema = MeeseeksDatabase.Schema)
        return MeeseeksDatabase(driver, taskAdapter, taskLogAdapter)
    }
}