package dev.mattramotar.meeseeks.runtime.impl

import dev.mattramotar.meeseeks.runtime.MeeseeksContext
import dev.mattramotar.meeseeks.runtime.MeeseeksRegistry
import dev.mattramotar.meeseeks.runtime.BackgroundTaskManager
import dev.mattramotar.meeseeks.runtime.BackgroundTaskConfig

internal expect class MeeseeksBoxFactory() {
    fun create(
        context: MeeseeksContext,
        registry: MeeseeksRegistry,
        config: BackgroundTaskConfig = BackgroundTaskConfig()
    ): BackgroundTaskManager
}