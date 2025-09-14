package dev.mattramotar.meeseeks.runtime

import dev.mattramotar.meeseeks.runtime.db.MeeseeksDatabase
import dev.mattramotar.meeseeks.runtime.impl.BackgroundTaskRunner
import dev.mattramotar.meeseeks.runtime.impl.MeeseeksAppDatabase
import dev.mattramotar.meeseeks.runtime.impl.WorkerRegistry


internal actual fun initializePlatformDependencies(
    context: AppContext,
    manager: BGTaskManager,
    registry: WorkerRegistry,
    config: BackgroundTaskConfig
) {
    val database: MeeseeksDatabase = MeeseeksAppDatabase.require(context)
    BackgroundTaskRunner.database = database
    BackgroundTaskRunner.registry = registry
    BackgroundTaskRunner.config = config
}