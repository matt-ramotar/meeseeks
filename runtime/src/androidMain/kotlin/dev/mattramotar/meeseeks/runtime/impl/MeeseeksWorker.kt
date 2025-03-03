package dev.mattramotar.meeseeks.runtime.impl

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dev.mattramotar.meeseeks.runtime.MeeseeksRegistry
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
    private val meeseeksRegistry: MeeseeksRegistry
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

        val attemptNumber = runAttemptCount
        val now = System.currentTimeMillis()
        taskQueries.updateStatus(TaskStatus.Running, now, mrMeeseeksId.value)

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
                    TaskStatus.Finished.Cancelled,
                    System.currentTimeMillis(),
                    taskEntity.id
                )
                Result.failure()
            }

            is TaskResult.Failure.Transient,
            TaskResult.Retry -> {
                Result.retry()
            }

            TaskResult.Success -> {
                taskQueries.updateStatus(
                    TaskStatus.Finished.Completed,
                    System.currentTimeMillis(),
                    taskEntity.id
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