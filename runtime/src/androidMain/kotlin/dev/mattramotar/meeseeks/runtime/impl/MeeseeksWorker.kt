package dev.mattramotar.meeseeks.runtime.impl

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dev.mattramotar.meeseeks.runtime.MeeseeksRegistry
import dev.mattramotar.meeseeks.runtime.MeeseeksTelemetry
import dev.mattramotar.meeseeks.runtime.MeeseeksTelemetryEvent
import dev.mattramotar.meeseeks.runtime.MrMeeseeks
import dev.mattramotar.meeseeks.runtime.MrMeeseeksId
import dev.mattramotar.meeseeks.runtime.Task
import dev.mattramotar.meeseeks.runtime.TaskResult
import dev.mattramotar.meeseeks.runtime.TaskStatus
import dev.mattramotar.meeseeks.runtime.db.MeeseeksDatabase
import dev.mattramotar.meeseeks.runtime.impl.extensions.TaskEntityExtensions.toTask
import dev.mattramotar.meeseeks.runtime.types.PermanentValidationException
import dev.mattramotar.meeseeks.runtime.types.TransientNetworkException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


internal class MeeseeksWorker(
    context: Context,
    workerParameters: WorkerParameters,
    private val database: MeeseeksDatabase,
    private val mrMeeseeksId: MrMeeseeksId,
    private val meeseeksRegistry: MeeseeksRegistry,
    private val telemetry: MeeseeksTelemetry? = null
) : CoroutineWorker(context, workerParameters) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val taskQueries = database.taskQueries
        val taskLogQueries = database.taskLogQueries

        val taskEntity =
            taskQueries.selectTaskByMrMeeseeksId(mrMeeseeksId.value).executeAsOneOrNull()
                ?: return@withContext Result.failure()

        if (taskEntity.status !is TaskStatus.Pending) {
            return@withContext Result.failure()
        }


        val now = System.currentTimeMillis()
        taskQueries.updateStatus(TaskStatus.Running, now, mrMeeseeksId.value)
        val attemptNumber = runAttemptCount

        telemetry?.onEvent(
            MeeseeksTelemetryEvent.TaskStarted(
                taskId = mrMeeseeksId,
                task = taskEntity.toTask(),
                runAttemptCount = attemptNumber,
            )
        )

        val result: TaskResult = try {
            val meeseeks =
                summonMrMeeseeks(taskEntity.toTask())
            meeseeks.execute(taskEntity.parameters)
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
            attempt = attemptNumber.toLong(),
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
                    MeeseeksTelemetryEvent.TaskFailed(
                        taskId = mrMeeseeksId,
                        task = taskEntity.toTask(),
                        error = result.error,
                        runAttemptCount = attemptNumber,
                    )
                )

                Result.failure()
            }

            is TaskResult.Failure.Transient -> {
                telemetry?.onEvent(
                    MeeseeksTelemetryEvent.TaskFailed(
                        taskId = mrMeeseeksId,
                        task = taskEntity.toTask(),
                        error = result.error,
                        runAttemptCount = attemptNumber,
                    )
                )
                Result.retry()
            }

            TaskResult.Retry -> {
                telemetry?.onEvent(
                    MeeseeksTelemetryEvent.TaskFailed(
                        taskId = mrMeeseeksId,
                        task = taskEntity.toTask(),
                        error = null,
                        runAttemptCount = attemptNumber,
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
                    MeeseeksTelemetryEvent.TaskSucceeded(
                        taskId = mrMeeseeksId,
                        task = taskEntity.toTask(),
                        runAttemptCount = attemptNumber,
                    )
                )

                Result.success()
            }
        }
    }

    private fun summonMrMeeseeks(task: Task): MrMeeseeks {
        val factory = meeseeksRegistry.getFactory(task.meeseeksType)
        return factory.create(task)
    }
}