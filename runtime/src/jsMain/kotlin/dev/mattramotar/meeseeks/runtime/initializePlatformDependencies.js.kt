package dev.mattramotar.meeseeks.runtime

import dev.mattramotar.meeseeks.runtime.db.MeeseeksDatabase
import dev.mattramotar.meeseeks.runtime.internal.BGTaskRunner
import dev.mattramotar.meeseeks.runtime.internal.MeeseeksAppDatabase
import dev.mattramotar.meeseeks.runtime.internal.WorkerRegistry


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