package dev.mattramotar.meeseeks.runtime

/**
 * Configuration for [BGTaskManager].
 */
data class BGTaskManagerConfig(
    val maxParallelTasks: Int = 2,
    val allowExpedited: Boolean = false,
    val maxRetryCount: Int = 3,
    val backoffMinimumMillis: Long = 10_000,
    val telemetry: TaskTelemetry? = null
)

