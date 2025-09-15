package dev.mattramotar.meeseeks.runtime.internal

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dev.mattramotar.meeseeks.runtime.BGTaskManagerConfig
import dev.mattramotar.meeseeks.runtime.TaskId
import dev.mattramotar.meeseeks.runtime.db.MeeseeksDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


internal class BGTaskCoroutineWorker(
    context: Context,
    workerParameters: WorkerParameters,
) : CoroutineWorker(context, workerParameters) {

    companion object {
        private const val TAG = "MeeseeksWorker"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {

        if (!BGTaskManagerSingleton.isInitialized()) {
            Log.w(TAG, "Worker attempting to run before Meeseeks initialization. Retrying later.")
            return@withContext Result.retry()
        }

        val database: MeeseeksDatabase = try {
            MeeseeksDatabaseSingleton.instance
        } catch (e: IllegalStateException) {
            Log.e(TAG, "Database unavailable.", e)
            return@withContext Result.retry()
        }

        val manager = BGTaskManagerSingleton.instance as? RealBGTaskManager
        if (manager == null) {
            Log.w(TAG, "BGTaskManager instance unavailable or wrong type.")
            return@withContext Result.retry()
        }

        val workerRegistry = manager.registry
        val telemetry = manager.config.telemetry

        val taskIdLong = inputData.getLong(WorkRequestFactory.KEY_TASK_ID, -1)
        if (taskIdLong == -1L) {
            Log.e(TAG, "Worker started without KEY_TASK_ID.")
            return@withContext Result.failure()
        }

        // Use the centralized TaskExecutor for consistent state management
        val executionResult = TaskExecutor.execute(
            taskId = taskIdLong,
            database = database,
            registry = workerRegistry,
            appContext = applicationContext,
            config = BGTaskManagerConfig(telemetry = telemetry),
            attemptCount = runAttemptCount
        )

        // Convert TaskExecutor result to WorkManager result
        return@withContext when (executionResult) {
            TaskExecutor.ExecutionResult.Success -> {
                Log.d(TAG, "Task $taskIdLong completed successfully")
                Result.success()
            }

            TaskExecutor.ExecutionResult.PlatformRetry -> {
                Log.d(TAG, "Task $taskIdLong requesting retry")
                Result.retry()
            }

            TaskExecutor.ExecutionResult.Failure -> {
                Log.e(TAG, "Task $taskIdLong failed permanently")
                Result.failure()
            }

            is TaskExecutor.ExecutionResult.PeriodicReschedule -> {
                // Periodic tasks are handled by WorkManager's periodic scheduling
                // We just need to signal success to continue the periodic chain
                Log.d(TAG, "Periodic task $taskIdLong completed, will run again per schedule")
                Result.success()
            }
        }
    }
}