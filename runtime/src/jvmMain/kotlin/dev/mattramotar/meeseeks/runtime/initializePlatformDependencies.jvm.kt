package dev.mattramotar.meeseeks.runtime

import dev.mattramotar.meeseeks.runtime.impl.WorkerRegistry


internal actual fun initializePlatformDependencies(
    context: AppContext,
    manager: BGTaskManager,
    registry: WorkerRegistry,
    config: BackgroundTaskConfig
) {
}