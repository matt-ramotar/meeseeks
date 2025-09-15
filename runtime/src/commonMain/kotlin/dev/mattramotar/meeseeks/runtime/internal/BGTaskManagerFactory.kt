package dev.mattramotar.meeseeks.runtime.internal

import dev.mattramotar.meeseeks.runtime.AppContext
import dev.mattramotar.meeseeks.runtime.BGTaskManager
import dev.mattramotar.meeseeks.runtime.BGTaskManagerConfig
import kotlinx.serialization.json.Json

internal expect class BGTaskManagerFactory() {
    fun create(
        context: AppContext,
        registry: WorkerRegistry,
        json: Json,
        config: BGTaskManagerConfig = BGTaskManagerConfig()
    ): BGTaskManager
}