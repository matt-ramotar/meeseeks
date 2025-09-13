package dev.mattramotar.meeseeks.runtime

actual fun initializePlatformSpecificScheduling(
    context: MeeseeksContext,
    config: BackgroundTaskConfig,
    backgroundTaskManager: BackgroundTaskManager,
    registry: MeeseeksRegistry
) {
    // No platform-specific scheduling needed
}