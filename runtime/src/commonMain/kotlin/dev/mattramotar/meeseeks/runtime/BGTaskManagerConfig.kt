package dev.mattramotar.meeseeks.runtime

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Configuration for [BGTaskManager].
 */
data class BGTaskManagerConfig(
    val maxParallelTasks: Int = 2,
    val allowExpedited: Boolean = false,
    val maxRetryCount: Int = 3,
    val minBackoff: Duration = 10.seconds,
    val telemetry: TaskTelemetry? = null
)

