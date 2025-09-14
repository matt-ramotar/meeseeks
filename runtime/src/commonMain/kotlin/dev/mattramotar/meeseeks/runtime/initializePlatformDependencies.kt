package dev.mattramotar.meeseeks.runtime

import dev.mattramotar.meeseeks.runtime.impl.WorkerRegistry

internal expect fun initializePlatformDependencies(
    context: AppContext,
    manager: BGTaskManager,
    registry: WorkerRegistry,
    config: BGTaskManagerConfig = BGTaskManagerConfig(),
)