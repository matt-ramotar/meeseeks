package dev.mattramotar.meeseeks.runtime.internal

import dev.mattramotar.meeseeks.runtime.BGTaskManagerConfig
import dev.mattramotar.meeseeks.runtime.EmptyAppContext
import dev.mattramotar.meeseeks.runtime.db.MeeseeksDatabase
import dev.mattramotar.meeseeks.runtime.internal.coroutines.MeeseeksDispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

object BGTaskRunner : CoroutineScope by CoroutineScope(MeeseeksDispatchers.IO) {

    internal lateinit var database: MeeseeksDatabase
    internal lateinit var registry: WorkerRegistry
    internal var config: BGTaskManagerConfig? = null

    // Retry configuration for JS platform
    private const val MAX_RETRY_ATTEMPTS = 5
    private val BASE_RETRY_DELAY = 1.seconds

    fun run(tag: String) {
        val taskId = WorkRequestFactory.taskIdFrom(tag)
        launch {
            executeTask(taskId, attemptCount = 1)
        }
    }

    private suspend fun executeTask(taskId: Long, attemptCount: Int) {
        // Use the centralized TaskExecutor for consistent state management
        val executionResult = TaskExecutor.execute(
            taskId = taskId,
            database = database,
            registry = registry,
            appContext = EmptyAppContext(),
            config = config,
            attemptCount = attemptCount
        )

        // Handle the execution result for JS platform
        when (executionResult) {
            TaskExecutor.ExecutionResult.Success -> {
                console.log("Task $taskId completed successfully")
            }

            TaskExecutor.ExecutionResult.PlatformRetry -> {
                // JS platform needs to handle retry manually
                if (attemptCount < MAX_RETRY_ATTEMPTS) {
                    // Exponential backoff with jitter
                    val delayMs = (BASE_RETRY_DELAY.inWholeMilliseconds * (1 shl (attemptCount - 1)))
                        .coerceAtMost(30_000) // Max 30 seconds
                    val jitter = (0..1000).random()
                    val totalDelay = delayMs + jitter

                    console.log("Task $taskId will retry after ${totalDelay}ms (attempt ${attemptCount + 1}/$MAX_RETRY_ATTEMPTS)")

                    delay(totalDelay)

                    // Retry the task
                    executeTask(taskId, attemptCount + 1)
                } else {
                    console.error("Task $taskId exceeded max retry attempts ($MAX_RETRY_ATTEMPTS)")
                    // Mark as failed after max retries
                    markTaskAsFailed(taskId)
                }
            }

            TaskExecutor.ExecutionResult.Failure -> {
                console.error("Task $taskId failed permanently")
            }

            is TaskExecutor.ExecutionResult.PeriodicReschedule -> {
                // For periodic tasks in JS, we need to schedule the next execution
                console.log("Periodic task $taskId completed, scheduling next execution")
                schedulePeriodicTask(executionResult.taskId, executionResult.request)
            }
        }
    }

    private fun markTaskAsFailed(taskId: Long) {
        database.taskSpecQueries.updateState(
            state = dev.mattramotar.meeseeks.runtime.internal.db.model.TaskState.FAILED,
            updated_at_ms = Timestamp.now(),
            id = taskId
        )
    }

    private fun schedulePeriodicTask(taskId: Long, request: dev.mattramotar.meeseeks.runtime.TaskRequest) {
        val schedule = request.schedule as? dev.mattramotar.meeseeks.runtime.TaskSchedule.Periodic ?: return

        launch {
            // Wait for the periodic interval
            delay(schedule.interval)

            // Execute the task again
            executeTask(taskId, attemptCount = 1)
        }
    }
}