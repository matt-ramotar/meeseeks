package dev.mattramotar.meeseeks.runtime

import dev.mattramotar.meeseeks.runtime.db.MeeseeksDatabase
import dev.mattramotar.meeseeks.runtime.internal.MeeseeksDatabaseSingleton
import dev.mattramotar.meeseeks.runtime.internal.WorkerRegistry
import kotlinx.serialization.json.Json

internal actual fun initializePlatformDependencies(
    context: AppContext,
    manager: BGTaskManager,
    registry: WorkerRegistry,
    json: Json,
    config: BGTaskManagerConfig
) {
    val database: MeeseeksDatabase = MeeseeksDatabaseSingleton.instance
    BGTaskRunner.database = database
    BGTaskRunner.registry = registry
    BGTaskRunner.config = config
}