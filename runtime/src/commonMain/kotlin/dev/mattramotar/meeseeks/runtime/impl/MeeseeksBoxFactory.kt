package dev.mattramotar.meeseeks.runtime.impl

import dev.mattramotar.meeseeks.runtime.AppContext
import dev.mattramotar.meeseeks.runtime.MeeseeksRegistry
import dev.mattramotar.meeseeks.runtime.BackgroundTaskManager
import dev.mattramotar.meeseeks.runtime.BackgroundTaskConfig

internal expect class MeeseeksBoxFactory() {
    fun create(
        context: AppContext,
        registry: MeeseeksRegistry,
        config: BackgroundTaskConfig = BackgroundTaskConfig()
    ): BackgroundTaskManager
}