package dev.mattramotar.meeseeks.core

/**
 * Configuration for [MrMeeseeksBox].
 */
data class MrMeeseeksBoxConfig(
    val maxParallelTasks: Int = 2,
    val allowExpedited: Boolean = false,
    val maxRetryCount: Int = 3,
    val backoffMinimumMillis: Long = 10_000
)

