package dev.mattramotar.meeseeks.runtime.internal

import dev.mattramotar.meeseeks.runtime.AppContext
import dev.mattramotar.meeseeks.runtime.BGTaskManager
import dev.mattramotar.meeseeks.runtime.BGTaskManagerConfig

internal expect class BGTaskManagerFactory() {
    fun create(
        context: AppContext,
        registry: WorkerRegistry,
        config: BGTaskManagerConfig = BGTaskManagerConfig()
    ): BGTaskManager
}