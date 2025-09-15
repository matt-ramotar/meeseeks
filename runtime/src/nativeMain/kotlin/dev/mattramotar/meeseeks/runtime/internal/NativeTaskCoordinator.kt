package dev.mattramotar.meeseeks.runtime.internal

import dev.mattramotar.meeseeks.runtime.BGTaskIdentifiers
import dev.mattramotar.meeseeks.runtime.BGTaskRunner
import dev.mattramotar.meeseeks.runtime.TaskSchedule
import dev.mattramotar.meeseeks.runtime.TaskPreconditions
import dev.mattramotar.meeseeks.runtime.db.MeeseeksDatabase
import dev.mattramotar.meeseeks.runtime.db.TaskSpec
import dev.mattramotar.meeseeks.runtime.internal.coroutines.MeeseeksDispatchers
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import platform.BackgroundTasks.BGTask
import platform.Foundation.NSLog
import kotlin.time.Duration.Companion.seconds

/**
 * Manages the execution lifecycle when the OS provides a background execution window.
 */
@OptIn(ExperimentalForeignApi::class)
internal class NativeTaskCoordinator(
    private val database: MeeseeksDatabase,
    private val scheduler: NativeTaskScheduler
) {
    private val coordinatorScope = CoroutineScope(SupervisorJob() + MeeseeksDispatchers.IO)

    // We need to ensure only one execution window is processed at a time
    private val executionMutex = Mutex()

    fun coordinateExecution(task: BGTask) {
        coordinatorScope.launch {
            executionMutex.withLock {
                executeWithinWindow(task)
            }
        }
    }

    private suspend fun executeWithinWindow(task: BGTask) {
        val identifier = task.identifier
        NSLog("[Meeseeks] Execution window opened for: $identifier")

        // 1. Create a dedicated scope tied to this window
        val windowJob = Job()
        val windowScope = CoroutineScope(windowJob + MeeseeksDispatchers.IO)

        // 2. Register an expiration handler to cancel the scope
        task.expirationHandler = {
            NSLog("[Meeseeks] Execution window expiring for $identifier. Canceling scope.")
            windowScope.cancel(CancellationException("iOS background time expired"))

            // Inform the OS we didn't finish due to expiration
            task.setTaskCompletedWithSuccess(false)
        }

        var overallSuccess = true

        try {
            // 3. Find eligible tasks
            val pendingTasks = database.taskSpecQueries.selectAllPending().executeAsList()
            val eligibleTasks = filterAndSortEligibleTasks(pendingTasks, identifier)

            // 4. Execute tasks sequentially until the scope is canceled or the list is exhausted
            for (taskSpec in eligibleTasks) {
                if (!windowJob.isActive) {
                    overallSuccess = false
                    break
                }

                // Launch within windowScope so it respects cancellation
                val executionJob = windowScope.launch {

                    // TaskRunner handles the actual execution, atomic claiming, and result processing
                    val success = BGTaskRunner.runTask(taskSpec.id)

                    if (!success) {
                        // If a task fails transiently or cannot be claimed, mark the overall window as incomplete
                        overallSuccess = false
                    }
                }

                // Wait for the current task to finish before starting the next
                executionJob.join()
            }
        } catch (_: CancellationException) {
            // Expected if expiration occurs
            overallSuccess = false
        } catch (e: Throwable) {
            NSLog("[Meeseeks] Error during execution window $identifier: $e")
            overallSuccess = false
        } finally {
            // 5. Finalize the task if the expirationHandler hasn't already
            if (windowJob.isActive) {
                task.setTaskCompletedWithSuccess(overallSuccess)
            }

            // 6. Reschedule if necessary
            rescheduleIfPendingWorkRemains()
        }
    }

    private fun filterAndSortEligibleTasks(tasks: List<TaskSpec>, identifier: String): List<TaskSpec> {
        val eligible = if (identifier == BGTaskIdentifiers.REFRESH) {
            // If invoked via REFRESH, only run tasks that don't require Processing constraints
            tasks.filter {
                !it.requires_network && !it.requires_charging
            }
        } else {
            // If invoked via PROCESSING, we can run anything
            tasks
        }

        // Sort by Priority then CreatedAt
        return eligible.sortedWith(compareByDescending<TaskSpec> { it.priority }.thenBy { it.created_at_ms })
    }

    /**
     * Checks the database for remaining work and schedules the appropriate platform wakeup
     */
    private fun rescheduleIfPendingWorkRemains() {
        val remainingTasks = database.taskSpecQueries.selectAllPending().executeAsList()

        if (remainingTasks.isEmpty()) {
            return
        }

        // Determine the requirements for the remaining tasks
        var needsProcessing = false
        var needsRefresh = false

        for (task in remainingTasks) {
            if (task.requires_network || task.requires_charging) {
                needsProcessing = true
            } else {
                needsRefresh = true
            }
        }

        // Submit requests for the necessary identifiers
        // Using a placeholder schedule to request execution ASAP, relying on OS backoff
        val immediateSchedule = TaskSchedule.OneTime(0.seconds)

        if (needsProcessing) {
            // Use broad constraints to ensure the OS considers the request if any processing task is pending
            val processingPreconditions = TaskPreconditions(requiresNetwork = true, requiresCharging = true)
            val request = scheduler.createBGTaskRequest(processingPreconditions, immediateSchedule)
            scheduler.submitRequest(request)
        }

        if (needsRefresh) {
            val refreshPreconditions = TaskPreconditions()
            val request = scheduler.createBGTaskRequest(refreshPreconditions, immediateSchedule)
            scheduler.submitRequest(request)
        }
    }
}