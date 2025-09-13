package dev.mattramotar.meeseeks.runtime.impl

import dev.mattramotar.meeseeks.runtime.AppContext
import dev.mattramotar.meeseeks.runtime.TaskWorkerRegistry
import dev.mattramotar.meeseeks.runtime.BackgroundTaskManager
import dev.mattramotar.meeseeks.runtime.BackgroundTaskConfig

internal expect class BackgroundTaskManagerFactory() {
    fun create(
        context: AppContext,
        registry: TaskWorkerRegistry,
        config: BackgroundTaskConfig = BackgroundTaskConfig()
    ): BackgroundTaskManager
}