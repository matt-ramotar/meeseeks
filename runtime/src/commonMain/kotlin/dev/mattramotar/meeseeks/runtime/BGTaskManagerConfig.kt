package dev.mattramotar.meeseeks.runtime

import dev.mattramotar.meeseeks.runtime.telemetry.Telemetry
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import dev.mattramotar.meeseeks.runtime.internal.TaskExecutor

/**
 * Configuration for [BGTaskManager].
 *
 * @property orphanedTaskWatchdogInterval Interval for the orphaned task watchdog to check for tasks that are enqueued in the database
 *      but not scheduled with the platform. This can happen if the process dies after [TaskExecutor] updates the DB but before the
 *      platform worker schedules the next execution. Set to [Duration.ZERO] to disable the watchdog.
 * @property payloadCipher Optional cipher used to encrypt/decrypt task payloads stored in the database.
 */
data class BGTaskManagerConfig(
    val maxParallelTasks: Int = 2,
    val allowExpedited: Boolean = false,
    val maxRetryCount: Int = 3,
    val minBackoff: Duration = 10.seconds,
    val telemetry: Telemetry? = null,
    val payloadCipher: PayloadCipher? = null,
    val orphanedTaskWatchdogInterval: Duration = 5.minutes
)
