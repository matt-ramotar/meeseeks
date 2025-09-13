package dev.mattramotar.meeseeks.runtime

actual fun initializePlatformSpecificScheduling(
    context: MeeseeksContext,
    config: MeeseeksBoxConfig,
    backgroundTaskManager: BackgroundTaskManager,
    registry: MeeseeksRegistry
) {
    // No platform-specific scheduling needed
}