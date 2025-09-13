package dev.mattramotar.meeseeks.runtime

import dev.mattramotar.meeseeks.runtime.db.MeeseeksDatabase
import dev.mattramotar.meeseeks.runtime.impl.MeeseeksAppDatabase
import dev.mattramotar.meeseeks.runtime.impl.BackgroundTaskRunner

actual fun initializePlatformSpecificScheduling(
    context: AppContext,
    config: BackgroundTaskConfig,
    backgroundTaskManager: BackgroundTaskManager,
    registry: TaskWorkerRegistry
) {
    val database: MeeseeksDatabase = MeeseeksAppDatabase.require(context)
    BackgroundTaskRunner.database = database
    BackgroundTaskRunner.registry = registry
    BackgroundTaskRunner.config = config
}