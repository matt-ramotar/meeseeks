package dev.mattramotar.meeseeks.runtime.impl

import dev.mattramotar.meeseeks.runtime.AppContext
import dev.mattramotar.meeseeks.runtime.BGTaskManager
import dev.mattramotar.meeseeks.runtime.BackgroundTaskConfig
import dev.mattramotar.meeseeks.runtime.impl.WorkerRegistry

internal expect class BackgroundTaskManagerFactory() {
    fun create(
        context: AppContext,
        registry: WorkerRegistry,
        config: BackgroundTaskConfig = BackgroundTaskConfig()
    ): BGTaskManager
}