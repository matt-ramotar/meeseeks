package dev.mattramotar.meeseeks.runtime.internal


import dev.mattramotar.meeseeks.runtime.AppContext
import dev.mattramotar.meeseeks.runtime.BGTaskManager
import dev.mattramotar.meeseeks.runtime.BGTaskManagerConfig
import dev.mattramotar.meeseeks.runtime.db.MeeseeksDatabase
import kotlinx.serialization.json.Json


internal fun createBGTaskManager(
    context: AppContext,
    database: MeeseeksDatabase,
    registry: WorkerRegistry,
    json: Json,
    config: BGTaskManagerConfig = BGTaskManagerConfig(),
): BGTaskManager {

    val factory = BGTaskManagerFactory()

    return factory.create(
        context,
        database,
        registry,
        json,
        config
    )
}



