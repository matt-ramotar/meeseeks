package dev.mattramotar.meeseeks.runtime

import dev.mattramotar.meeseeks.runtime.db.MeeseeksDatabase
import dev.mattramotar.meeseeks.runtime.internal.MeeseeksAppDatabase
import dev.mattramotar.meeseeks.runtime.internal.WorkerRegistry
import kotlinx.serialization.json.Json

internal actual fun initializePlatformDependencies(
    context: AppContext,
    manager: BGTaskManager,
    registry: WorkerRegistry,
    json: Json,
    config: BGTaskManagerConfig
) {
    val database: MeeseeksDatabase = MeeseeksAppDatabase.require(context, json)
    BGTaskRunner.database = database
    BGTaskRunner.registry = registry
    BGTaskRunner.config = config
}