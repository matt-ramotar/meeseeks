@file:Suppress("UnsafeCastFromDynamic")

package dev.mattramotar.meeseeks.runtime.impl

import app.cash.sqldelight.driver.worker.WebWorkerDriver
import dev.mattramotar.meeseeks.runtime.AppContext
import dev.mattramotar.meeseeks.runtime.db.MeeseeksDatabase
import dev.mattramotar.meeseeks.runtime.db.TaskEntity
import dev.mattramotar.meeseeks.runtime.db.TaskLogEntity
import org.w3c.dom.Worker

internal actual class MeeseeksDatabaseFactory actual constructor() {

    actual fun create(
        context: AppContext,
        taskAdapter: TaskEntity.Adapter,
        taskLogAdapter: TaskLogEntity.Adapter
    ): MeeseeksDatabase {

        val driver = WebWorkerDriver(
            Worker(
                js("""new URL("@cashapp/sqldelight-sqljs-worker/sqljs.worker.js", import.meta.url)""")
            )
        )

        return MeeseeksDatabase(
            driver = driver,
            taskEntityAdapter = taskAdapter,
            taskLogEntityAdapter = taskLogAdapter
        )
    }
}