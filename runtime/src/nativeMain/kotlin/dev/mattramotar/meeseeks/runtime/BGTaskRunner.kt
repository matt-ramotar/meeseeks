package dev.mattramotar.meeseeks.runtime


import dev.mattramotar.meeseeks.runtime.db.MeeseeksDatabase
import dev.mattramotar.meeseeks.runtime.internal.TaskExecutor
import dev.mattramotar.meeseeks.runtime.internal.Timestamp
import dev.mattramotar.meeseeks.runtime.internal.WorkerRegistry
import dev.mattramotar.meeseeks.runtime.internal.coroutines.MeeseeksDispatchers
import dev.mattramotar.meeseeks.runtime.internal.db.model.TaskState
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import platform.BackgroundTasks.BGAppRefreshTaskRequest
import platform.BackgroundTasks.BGProcessingTaskRequest
import platform.BackgroundTasks.BGTaskRequest
import platform.BackgroundTasks.BGTaskScheduler
import platform.Foundation.NSDate
import platform.Foundation.dateWithTimeIntervalSinceNow


object BGTaskRunner : CoroutineScope by CoroutineScope(MeeseeksDispatchers.IO) {

    internal lateinit var database: MeeseeksDatabase
    internal lateinit var registry: WorkerRegistry
    internal var config: BGTaskManagerConfig? = null

    fun run(
        id: Long,
        completionCallback: (Boolean) -> Unit
    ) {
        launch {
            try {
                val succeeded = runTask(id)
                completionCallback(succeeded)
            } catch (_: Throwable) {
                completionCallback(false)
            }
        }
    }

    internal suspend fun runTask(id: Long): Boolean {
        // Use the centralized TaskExecutor for consistent state management
        val executionResult = TaskExecutor.execute(
            taskId = id,
            database = database,
            registry = registry,
            appContext = EmptyAppContext(),
            config = config,
            attemptCount = 0 // Native platform doesn't track attempts
        )

        // Handle the execution result for native platform
        return when (executionResult) {
            TaskExecutor.ExecutionResult.Success -> {
                // Task completed successfully
                true
            }

            TaskExecutor.ExecutionResult.PlatformRetry -> {
                // Native platform will handle retry through OS scheduling
                // The task state has been reset to ENQUEUED by TaskExecutor
                // Return false to indicate the task needs retry
                false
            }

            TaskExecutor.ExecutionResult.Failure -> {
                // Permanent failure
                false
            }

            is TaskExecutor.ExecutionResult.PeriodicReschedule -> {
                // Handle periodic task rescheduling
                resubmitPeriodic(
                    executionResult.taskId,
                    executionResult.request,
                    database.taskSpecQueries.selectTaskById(id).executeAsOne().run_attempt_count.toInt()
                )
                true
            }
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun resubmitPeriodic(
        taskId: Long,
        request: TaskRequest,
        attemptCount: Int
    ) {
        val schedule = request.schedule as? TaskSchedule.Periodic ?: return

        val identifier = if (request.preconditions.requiresNetwork || request.preconditions.requiresCharging) {
            BGTaskIdentifiers.PROCESSING
        } else {
            BGTaskIdentifiers.REFRESH
        }

        val bgTaskRequest = createNextBGTaskRequest(identifier, request, schedule)
        BGTaskScheduler.sharedScheduler.submitTaskRequest(bgTaskRequest, null)

        val now = Timestamp.now()
        database.taskSpecQueries.updatePlatformId(identifier, now, taskId)
        database.taskSpecQueries.updateState(TaskState.ENQUEUED, now, taskId)

        database.taskLogQueries.insertLog(
            taskId = taskId,
            created = now,
            result = TaskResult.Type.SuccessAndScheduledNext,
            attempt = attemptCount.toLong(),
            message = "Periodic task resubmitted by BGTaskRunner."
        )
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun createNextBGTaskRequest(
        identifier: String,
        request: TaskRequest,
        periodic: TaskSchedule.Periodic
    ): BGTaskRequest {
        val requiresNetwork = request.preconditions.requiresNetwork
        val requiresCharging = request.preconditions.requiresCharging

        val bgTaskRequest: BGTaskRequest = if (requiresNetwork || requiresCharging) {
            BGProcessingTaskRequest(identifier).apply {
                requiresNetworkConnectivity = requiresNetwork
                requiresExternalPower = requiresCharging
            }
        } else {
            BGAppRefreshTaskRequest(identifier)
        }

        val earliestSeconds = periodic.interval.inWholeSeconds.toDouble()
        if (earliestSeconds > 0.0) {
            bgTaskRequest.earliestBeginDate = NSDate.dateWithTimeIntervalSinceNow(earliestSeconds)
        }

        return bgTaskRequest
    }
}