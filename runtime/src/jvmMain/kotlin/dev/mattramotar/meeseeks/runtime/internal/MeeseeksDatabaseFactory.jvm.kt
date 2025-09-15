package dev.mattramotar.meeseeks.runtime.internal

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import dev.mattramotar.meeseeks.runtime.AppContext
import dev.mattramotar.meeseeks.runtime.db.MeeseeksDatabase
import dev.mattramotar.meeseeks.runtime.db.TaskLogEntity
import dev.mattramotar.meeseeks.runtime.db.TaskSpec
import java.sql.SQLException

internal actual class MeeseeksDatabaseFactory actual constructor() {
    actual fun create(
        context: AppContext,
        taskSpecAdapter: TaskSpec.Adapter,
        taskLogAdapter: TaskLogEntity.Adapter
    ): MeeseeksDatabase {
        val driver = JdbcSqliteDriver("jdbc:sqlite:meeseeks.db", schema = MeeseeksDatabase.Schema)
        val database = MeeseeksDatabase(driver, taskLogAdapter, taskSpecAdapter)
        runQuartzSchema(driver)
        return database
    }

    private fun runQuartzSchema(driver: JdbcSqliteDriver) {
        val resourcePath = "/dev/mattramotar/meeseeks/runtime/internal/quartz/tables_sqlite.sql"
        val stream = this::class.java.getResourceAsStream(resourcePath)
            ?: throw IllegalStateException("Could not find $resourcePath")

        val script = stream.bufferedReader().readText()
        val statements = script.split(";").map { it.trim() }.filter { it.isNotEmpty() }

        for (sql in statements) {
            try {
                driver.execute(null, sql, 0)
            } catch (_: SQLException) {
            }
        }
    }
}