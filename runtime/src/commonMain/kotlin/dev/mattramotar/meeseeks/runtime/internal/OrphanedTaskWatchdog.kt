package dev.mattramotar.meeseeks.runtime.internal

import dev.mattramotar.meeseeks.runtime.db.MeeseeksDatabase
import dev.mattramotar.meeseeks.runtime.db.TaskSpec
import dev.mattramotar.meeseeks.runtime.internal.db.TaskMapper
import dev.mattramotar.meeseeks.runtime.telemetry.Telemetry
import dev.mattramotar.meeseeks.runtime.telemetry.TelemetryEvent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration

/**
 * Periodically checks for orphaned tasks.
 *
 * An orphaned task is a task that is enqueued in DB but not scheduled with the platform. This can occur when the process dies
 * after [TaskExecutor] updates the DB but before the platform worker schedules the next execution.
 *
 * @param scope The coroutine scope to use for the watchdog. When the scope is canceled, the watchdog stops automatically.
 */
internal class OrphanedTaskWatchdog(
    private val scope: CoroutineScope,
    private val taskRescheduler: TaskRescheduler,
    private val taskScheduler: TaskScheduler,
    private val registry: WorkerRegistry,
    private val database: MeeseeksDatabase,
    private val interval: Duration,
    private val telemetry: Telemetry? = null
) {
    private var watchdogJob: Job? = null

    /**
     * Start the watchdog.
     * This is a no-op if the [interval] is not positive or the watchdog is already active.
     * The watchdog will automatically stop when the provided [scope] is canceled.
     */
    fun start() {
        if (!interval.isPositive() || watchdogJob?.isActive == true) {
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
        } catch (e: Throwable) {
            // Preserve cooperative cancellation
            if (e is CancellationException) throw e
            // Silently catch other errors to keep the watchdog alive.
            // Avoid platform-specific logging from the common source set.
            // Extend telemetry with a custom watchdog error event for monitoring if needed.
        }
    }
}
