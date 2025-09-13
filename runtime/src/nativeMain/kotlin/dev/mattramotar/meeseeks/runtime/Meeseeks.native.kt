package dev.mattramotar.meeseeks.runtime

import dev.mattramotar.meeseeks.runtime.db.MeeseeksDatabase
import dev.mattramotar.meeseeks.runtime.impl.MeeseeksAppDatabase

actual fun initializePlatformSpecificScheduling(
    context: MeeseeksContext,
    config: BackgroundTaskConfig,
    backgroundTaskManager: BackgroundTaskManager,
    registry: MeeseeksRegistry
) {
    val database: MeeseeksDatabase = MeeseeksAppDatabase.require(context)
    MeeseeksBGTaskRunner.database = database
    MeeseeksBGTaskRunner.registry = registry
    MeeseeksBGTaskRunner.config = config
}