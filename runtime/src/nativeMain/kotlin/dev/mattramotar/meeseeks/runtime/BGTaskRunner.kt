package dev.mattramotar.meeseeks.runtime


import dev.mattramotar.meeseeks.runtime.internal.MeeseeksDependencies
import dev.mattramotar.meeseeks.runtime.internal.TaskExecutor
import dev.mattramotar.meeseeks.runtime.internal.Timestamp
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
import kotlin.time.Duration


internal class BGTaskRunner(
    dependencies: MeeseeksDependencies
) : CoroutineScope by CoroutineScope(MeeseeksDispatchers.IO) {

    private val database = dependencies.database
    private val registry = dependencies.registry
    private val appContext = dependencies.appContext
    private val config = dependencies.config

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
            appContext = appContext,
            config = config,
            attemptCount = 0 // Native platform doesn't track attempts
        )

        // Handle the execution result for native platform
        return when (executionResult) {
            is TaskExecutor.ExecutionResult.ScheduleNextActivation -> {
                // This now handles both retries AND periodic tasks.
                resubmitTask(
                    executionResult.taskId,
                    executionResult.request,
                    executionResult.delay
                )
                true
            }

            TaskExecutor.ExecutionResult.Terminal.Failure,
            TaskExecutor.ExecutionResult.Terminal.Success -> true // Meeseeks is the single SOT for scheduling
        }
    }


    @OptIn(ExperimentalForeignApi::class)
    private fun resubmitTask(
        taskId: Long,
        request: TaskRequest,
        delay: Duration
    ) {
        val identifier = if (request.preconditions.requiresNetwork || request.preconditions.requiresCharging) {
            BGTaskIdentifiers.PROCESSING
        } else {
            BGTaskIdentifiers.REFRESH
        }

        val attemptCount = database.taskSpecQueries.selectTaskById(taskId).executeAsOne().run_attempt_count.toInt()

        val bgTaskRequest = createNextBGTaskRequest(identifier, request, delay)
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
        delay: Duration
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

        val earliestSeconds = delay.inWholeSeconds.toDouble()
        if (earliestSeconds > 0.0) {
            bgTaskRequest.earliestBeginDate = NSDate.dateWithTimeIntervalSinceNow(earliestSeconds)
        }

        return bgTaskRequest
    }
}