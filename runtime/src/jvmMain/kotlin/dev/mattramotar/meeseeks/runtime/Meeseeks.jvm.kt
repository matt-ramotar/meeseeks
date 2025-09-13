package dev.mattramotar.meeseeks.runtime

actual fun initializePlatformSpecificScheduling(
    context: AppContext,
    config: BackgroundTaskConfig,
    backgroundTaskManager: BackgroundTaskManager,
    registry: TaskWorkerRegistry
) {
    // No platform-specific scheduling needed
}