package dev.mattramotar.meeseeks.runtime.internal

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dev.mattramotar.meeseeks.runtime.BGTaskManagerConfig
import dev.mattramotar.meeseeks.runtime.db.MeeseeksDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


internal class BGTaskCoroutineWorker @JvmOverloads constructor(
    context: Context,
    workerParameters: WorkerParameters,
    private val dependencies: MeeseeksDependencies? = null
) : CoroutineWorker(context, workerParameters) {

    companion object {
        private const val TAG = "MeeseeksWorker"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        if (dependencies == null) {
            Log.w(TAG, "Worker attempting to run before Meeseeks initialization. Retrying later.")
            return@withContext Result.retry()
        }

        val database = dependencies.database
        val workerRegistry = dependencies.registry
        val config = dependencies.config
        val telemetry = config.telemetry

        val taskId = inputData.getString(WorkRequestFactory.KEY_TASK_ID)
        if (taskId.isNullOrBlank()) {
            Log.e(TAG, "Worker started without KEY_TASK_ID.")
            return@withContext Result.failure()
        }

        // Use the centralized TaskExecutor for consistent state management
        val executionResult = TaskExecutor.execute(
            taskId = taskId,
            database = database,
            registry = workerRegistry,
            appContext = applicationContext,
            config = BGTaskManagerConfig(telemetry = telemetry),
            attemptCount = runAttemptCount
        )

        // Convert TaskExecutor result to WorkManager result
        when (executionResult) {
            is TaskExecutor.ExecutionResult.ScheduleNextActivation -> {
                Log.d(TAG, "Task $taskId requires next activation in ${executionResult.delay.inWholeMilliseconds}ms.")
                scheduleNextActivation(executionResult, config, database)
            }

            TaskExecutor.ExecutionResult.Terminal.Failure -> {
                Log.d(TAG, "Task $taskId is terminal failure.")
            }

            TaskExecutor.ExecutionResult.Terminal.Success -> {
                Log.d(TAG, "Task $taskId is terminal success.")
            }
        }

        // Always return success. The current attempt is done. This prevents WorkManager from attempting any internal retry.
        return@withContext Result.success()
    }

    private fun scheduleNextActivation(
        result: TaskExecutor.ExecutionResult.ScheduleNextActivation,
        config: BGTaskManagerConfig,
        database: MeeseeksDatabase
    ) {
        val workManager = WorkManager.getInstance(applicationContext)
        val factory = WorkRequestFactory()

        // Create the new OneTimeWorkRequest with the calculated delay override.
        val nextWorkRequest = factory.createWorkRequest(
            result.taskId,
            result.request,
            config,
            delayOverrideMs = result.delay.inWholeMilliseconds
        )

        // Enqueue the next activation. Use REPLACE to ensure this new schedule takes precedence.
        workManager.enqueueUniqueWork(
            uniqueWorkName = WorkRequestFactory.uniqueWorkNameFor(result.taskId, result.request.schedule),
            existingWorkPolicy = ExistingWorkPolicy.REPLACE,
            request = nextWorkRequest.delegate as OneTimeWorkRequest
        )

        // Update the platform_id in the DB to track the new execution attempt ID.
        database.taskSpecQueries.updatePlatformId(
            platform_id = nextWorkRequest.id,
            updated_at_ms = Timestamp.now(),
            id = result.taskId
        )
    }
}
