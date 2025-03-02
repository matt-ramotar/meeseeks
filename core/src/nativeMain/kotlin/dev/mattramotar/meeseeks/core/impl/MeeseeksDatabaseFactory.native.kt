package dev.mattramotar.meeseeks.core.impl

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import dev.mattramotar.meeseeks.core.MeeseeksContext
import dev.mattramotar.meeseeks.core.db.MeeseeksDatabase
import dev.mattramotar.meeseeks.core.db.TaskEntity
import dev.mattramotar.meeseeks.core.db.TaskLogEntity
import dev.mattramotar.meeseeks.core.impl.db.adapters.taskEntityAdapter
import dev.mattramotar.meeseeks.core.impl.db.adapters.taskLogEntityAdapter
import kotlinx.serialization.json.Json

internal actual class MeeseeksDatabaseFactory actual constructor() {
    actual fun create(
        context: MeeseeksContext,
        taskAdapter: TaskEntity.Adapter,
        taskLogAdapter: TaskLogEntity.Adapter
    ): MeeseeksDatabase {
        val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
        }

        val driver: SqlDriver = NativeSqliteDriver(
            schema = MeeseeksDatabase.Schema,
            name = "meeseeks.db"
        )
        return MeeseeksDatabase(
            driver,
            taskEntityAdapter(json),
            taskLogEntityAdapter(json)
        )
    }
}