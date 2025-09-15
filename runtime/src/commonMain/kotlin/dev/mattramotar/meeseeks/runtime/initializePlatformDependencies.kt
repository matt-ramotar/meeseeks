package dev.mattramotar.meeseeks.runtime

import dev.mattramotar.meeseeks.runtime.internal.WorkerRegistry
import kotlinx.serialization.json.Json

internal expect fun initializePlatformDependencies(
    context: AppContext,
    manager: BGTaskManager,
    registry: WorkerRegistry,
    json: Json,
    config: BGTaskManagerConfig = BGTaskManagerConfig(),
)