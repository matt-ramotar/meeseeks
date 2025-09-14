package dev.mattramotar.meeseeks.runtime.internal

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dev.mattramotar.meeseeks.runtime.AppContext
import dev.mattramotar.meeseeks.runtime.TaskPayload
import dev.mattramotar.meeseeks.runtime.RuntimeContext
import dev.mattramotar.meeseeks.runtime.TaskId
import dev.mattramotar.meeseeks.runtime.TaskRequest
import dev.mattramotar.meeseeks.runtime.TaskResult
import dev.mattramotar.meeseeks.runtime.TaskStatus
import dev.mattramotar.meeseeks.runtime.TaskTelemetry
import dev.mattramotar.meeseeks.runtime.TaskTelemetryEvent
import dev.mattramotar.meeseeks.runtime.Worker
import dev.mattramotar.meeseeks.runtime.db.MeeseeksDatabase
import dev.mattramotar.meeseeks.runtime.internal.extensions.TaskEntityExtensions.toTaskRequest
import dev.mattramotar.meeseeks.runtime.types.PermanentValidationException
import dev.mattramotar.meeseeks.runtime.types.TransientNetworkException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


internal class BGTaskCoroutineWorker(
    context: Context,
    workerParameters: WorkerParameters,
    private val database: MeeseeksDatabase,
    private val taskId: TaskId,
    private val workerRegistry: WorkerRegistry,
    private val telemetry: TaskTelemetry? = null,
    private val appContext: AppContext = context.applicationContext
) : CoroutineWorker(context, workerParameters) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val taskQueries = database.taskQueries
        val taskLogQueries = database.taskLogQueries

        val taskEntity =
            taskQueries.selectTaskByTaskId(taskId.value).executeAsOneOrNull()
                ?: return@withContext Result.failure()

        if (taskEntity.status !is TaskStatus.Pending) {
            return@withContext Result.failure()
        }

        val now = System.currentTimeMillis()
        taskQueries.updateStatus(TaskStatus.Running, now, taskId.value)
        val attemptCount = runAttemptCount

        telemetry?.onEvent(
            TaskTelemetryEvent.TaskStarted(
                taskId = taskId,
                task = taskEntity.toTaskRequest(),
                runAttemptCount = attemptCount,
            )
        )

        val result: TaskResult = try {
            val request = taskEntity.toTaskRequest()
            val worker = getWorker(request)
            worker.run(payload = request.payload, context = RuntimeContext(attemptCount))
        } catch (error: Throwable) {
            when (error) {
                is TransientNetworkException -> TaskResult.Failure.Transient(error)
                is PermanentValidationException -> TaskResult.Failure.Permanent(error)
                else -> TaskResult.Failure.Permanent(error)
            }
        }

        taskLogQueries.insertLog(
            taskId = taskEntity.id,
            created = now,
            result = result.type,
            attempt = attemptCount.toLong(),
            message = null
        )

        when (result) {
            is TaskResult.Failure.Permanent -> {
                taskQueries.updateStatus(
                    TaskStatus.Finished.Failed,
                    Timestamp.now(),
                    taskEntity.id
                )

                telemetry?.onEvent(
                    TaskTelemetryEvent.TaskFailed(
                        taskId = taskId,
                        task = taskEntity.toTaskRequest(),
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
                        task = taskEntity.toTaskRequest(),
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
                        task = taskEntity.toTaskRequest(),
                        error = null,
                        runAttemptCount = attemptCount,
                    )
                )
                Result.retry()
            }

            TaskResult.Success -> {
                taskQueries.updateStatus(
                    TaskStatus.Finished.Completed,
                    Timestamp.now(),
                    taskEntity.id
                )

                telemetry?.onEvent(
                    TaskTelemetryEvent.TaskSucceeded(
                        taskId = taskId,
                        task = taskEntity.toTaskRequest(),
                        runAttemptCount = attemptCount,
                    )
                )

                Result.success()
            }
        }
    }

    private fun getWorker(request: TaskRequest): Worker<TaskPayload> {
        val factory = workerRegistry.getFactory(request.payload::class)
        @Suppress("UNCHECKED_CAST")
        return factory.create(appContext) as Worker<TaskPayload>
    }
}