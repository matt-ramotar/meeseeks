package dev.mattramotar.meeseeks.runtime

actual fun initializePlatformSpecificScheduling(
    context: MeeseeksContext,
    config: MeeseeksBoxConfig,
    meeseeksBox: MeeseeksBox,
    registry: MeeseeksRegistry
) {
    // No platform-specific scheduling needed
}