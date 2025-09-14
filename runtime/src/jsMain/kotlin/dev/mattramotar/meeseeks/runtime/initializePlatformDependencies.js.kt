package dev.mattramotar.meeseeks.runtime

import dev.mattramotar.meeseeks.runtime.db.MeeseeksDatabase
import dev.mattramotar.meeseeks.runtime.impl.BGTaskRunner
import dev.mattramotar.meeseeks.runtime.impl.MeeseeksAppDatabase
import dev.mattramotar.meeseeks.runtime.impl.WorkerRegistry


internal actual fun initializePlatformDependencies(
    context: AppContext,
    manager: BGTaskManager,
    registry: WorkerRegistry,
    config: BGTaskManagerConfig
) {
    val database: MeeseeksDatabase = MeeseeksAppDatabase.require(context)
    BGTaskRunner.database = database
    BGTaskRunner.registry = registry
    BGTaskRunner.config = config
}