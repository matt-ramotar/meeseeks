package dev.mattramotar.meeseeks.runtime.internal

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dev.mattramotar.meeseeks.runtime.AppContext
import dev.mattramotar.meeseeks.runtime.RuntimeContext
import dev.mattramotar.meeseeks.runtime.TaskId
import dev.mattramotar.meeseeks.runtime.TaskPayload
import dev.mattramotar.meeseeks.runtime.TaskRequest
import dev.mattramotar.meeseeks.runtime.TaskResult
import dev.mattramotar.meeseeks.runtime.TaskTelemetryEvent
import dev.mattramotar.meeseeks.runtime.Worker
import dev.mattramotar.meeseeks.runtime.db.MeeseeksDatabase
import dev.mattramotar.meeseeks.runtime.db.TaskSpec
import dev.mattramotar.meeseeks.runtime.internal.db.TaskMapper
import dev.mattramotar.meeseeks.runtime.internal.db.model.TaskState
import dev.mattramotar.meeseeks.runtime.types.PermanentValidationException
import dev.mattramotar.meeseeks.runtime.types.TransientNetworkException
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

        val appContext = applicationContext
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
        val taskId = TaskId(taskIdLong)

        val taskSpecQueries = database.taskSpecQueries
        val taskLogQueries = database.taskLogQueries

        val taskSpec = database.claimTask(taskIdLong) ?: return@withContext Result.failure()
        val attemptCount = runAttemptCount

        val taskRequest = TaskMapper.mapToTaskRequest(taskSpec, workerRegistry)

        telemetry?.onEvent(
            TaskTelemetryEvent.TaskStarted(
                taskId = taskId,
                task = taskRequest,
                runAttemptCount = attemptCount,
            )
        )

        val result: TaskResult = try {
            val worker = getWorker(taskRequest, workerRegistry, appContext)
            worker.run(payload = taskRequest.payload, context = RuntimeContext(attemptCount))
        } catch (error: Throwable) {
            when (error) {
                is TransientNetworkException -> TaskResult.Failure.Transient(error)
                is PermanentValidationException -> TaskResult.Failure.Permanent(error)
                else -> TaskResult.Failure.Permanent(error)
            }
        }

        taskLogQueries.insertLog(
            taskId = taskSpec.id,
            created = Timestamp.now(),
            result = result.type,
            attempt = attemptCount.toLong(),
            message = null
        )

        when (result) {
            is TaskResult.Failure.Permanent -> {
                taskSpecQueries.updateState(
                    TaskState.FAILED,
                    Timestamp.now(),
                    taskSpec.id
                )

                telemetry?.onEvent(
                    TaskTelemetryEvent.TaskFailed(
                        taskId = taskId,
                        task = taskRequest,
                        error = result.error,
                        runAttemptCount = attemptCount,
                    )
                )

                Result.failure()
            }

            is TaskResult.Failure.Transient -> {
                telemetry?.onEvent(
                    TaskTelemetryEvent.TaskFailed(
                        taskId = taskId,
                        task = taskRequest,
                        error = result.error,
                        runAttemptCount = attemptCount,
                    )
                )
                Result.retry()
            }

            TaskResult.Retry -> {
                telemetry?.onEvent(
                    TaskTelemetryEvent.TaskFailed(
                        taskId = taskId,
                        task = taskRequest,
                        error = null,
                        runAttemptCount = attemptCount,
                    )
                )
                Result.retry()
            }

            TaskResult.Success -> {
                taskSpecQueries.updateState(
                    TaskState.SUCCEEDED,
                    Timestamp.now(),
                    taskSpec.id
                )

                telemetry?.onEvent(
                    TaskTelemetryEvent.TaskSucceeded(
                        taskId = taskId,
                        task = taskRequest,
                        runAttemptCount = attemptCount,
                    )
                )

                Result.success()
            }
        }
    }

    private fun getWorker(request: TaskRequest, workerRegistry: WorkerRegistry, appContext: AppContext): Worker<TaskPayload> {
        val factory = workerRegistry.getFactory(request.payload::class)
        @Suppress("UNCHECKED_CAST")
        return factory.create(appContext) as Worker<TaskPayload>
    }

    private fun MeeseeksDatabase.claimTask(id: Long): TaskSpec? =
        taskSpecQueries.transactionWithResult {
            taskSpecQueries.atomicallyClaimAndStartTask(id, Timestamp.now())
            val rowsAffected = taskSpecQueries.selectChanges().executeAsOne().toInt()
            if (rowsAffected == 0) {
                null
            } else {
                taskSpecQueries.selectTaskById(id).executeAsOneOrNull()
            }
        }
}