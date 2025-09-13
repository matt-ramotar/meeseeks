package dev.mattramotar.meeseeks.runtime

expect fun initializePlatformSpecificScheduling(
    context: AppContext,
    config: BackgroundTaskConfig = BackgroundTaskConfig(),
    backgroundTaskManager: BackgroundTaskManager,
    registry: TaskWorkerRegistry
)