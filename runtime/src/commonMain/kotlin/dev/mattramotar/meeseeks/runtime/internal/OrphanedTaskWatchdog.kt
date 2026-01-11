package dev.mattramotar.meeseeks.runtime.internal

import dev.mattramotar.meeseeks.runtime.db.MeeseeksDatabase
import dev.mattramotar.meeseeks.runtime.internal.coroutines.MeeseeksDispatchers
import dev.mattramotar.meeseeks.runtime.internal.db.TaskMapper
import dev.mattramotar.meeseeks.runtime.telemetry.Telemetry
import dev.mattramotar.meeseeks.runtime.telemetry.TelemetryEvent
import dev.mattramotar.meeseeks.runtime.db.TaskSpec
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration

/**
 * Periodically checks for orphaned tasks.
 *
 * An orphaned task is a task that is enqueued in DB but not scheduled with the platform. This can occur when the process dies
 * after [TaskExecutor] updates the DB but before the platform worker schedules the next execution.
 */
internal class OrphanedTaskWatchdog(
    private val taskRescheduler: TaskRescheduler,
    private val taskScheduler: TaskScheduler,
    private val registry: WorkerRegistry,
    private val database: MeeseeksDatabase,
    private val interval: Duration,
    private val telemetry: Telemetry? = null
) {
    private val scope = CoroutineScope(SupervisorJob() + MeeseeksDispatchers.IO)
    private var watchdogJob: Job? = null

    /**
     * Start the watchdog.
     * This is a no-op if the [interval] is zero or the watchdog is already active.
     */
    fun start() {
        if (interval == Duration.ZERO || watchdogJob?.isActive == true) {
            return
        }

        watchdogJob = scope.launch {
            while (isActive) {
                delay(interval)
                checkForOrphanedTasks()
            }
        }
    }

    /**
     * Stop the watchdog and cancel its coroutine scope.
     */
    fun stop() {
        scope.cancel()
    }

    /**
     * Check for and recover orphaned tasks.
     *
     * Only reschedules tasks that are:
     * 1. In the enqueued state
     * 2. Have [TaskSpec.next_run_time_ms] in the past (overdue)
     * 3. Not currently scheduled with the platform scheduler
     */
    private suspend fun checkForOrphanedTasks() {
        try {
            val now = Timestamp.now()
            val overdueTasks = database.taskSpecQueries.selectOverdueTasks(now).executeAsList()

            var recoveredCount = 0
            for (taskSpec in overdueTasks) {
                val taskRequest = TaskMapper.mapToTaskRequest(taskSpec, registry)
                if (!taskScheduler.isScheduled(taskSpec.id, taskRequest.schedule)) {
                    taskRescheduler.rescheduleTask(taskSpec)
                    recoveredCount++
                }
            }

            if (recoveredCount > 0) {
                telemetry?.onEvent(TelemetryEvent.OrphanedTasksRecovered(count = recoveredCount))
            }
        } catch (_: Throwable) {
            // Silently catch errors to keep the watchdog alive
            // Avoid platform-specific logging from common main
            // Extend telemetry with a custom Watchdog error event for platform-specific monitoring
        }
    }
}
