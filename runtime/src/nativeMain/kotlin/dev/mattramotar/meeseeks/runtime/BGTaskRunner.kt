package dev.mattramotar.meeseeks.runtime


import dev.mattramotar.meeseeks.runtime.db.MeeseeksDatabase
import dev.mattramotar.meeseeks.runtime.impl.Timestamp
import dev.mattramotar.meeseeks.runtime.impl.WorkRequestFactory
import dev.mattramotar.meeseeks.runtime.impl.coroutines.MeeseeksDispatchers
import dev.mattramotar.meeseeks.runtime.impl.extensions.TaskEntityExtensions.toTask
import dev.mattramotar.meeseeks.runtime.types.PermanentValidationException
import dev.mattramotar.meeseeks.runtime.types.TransientNetworkException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch


object BGTaskRunner: CoroutineScope by CoroutineScope(MeeseeksDispatchers.IO) {

    internal lateinit var database: MeeseeksDatabase
    internal lateinit var registry: TaskWorkerRegistry
    internal var config: BackgroundTaskConfig? = null


    fun run(
        bgTaskIdentifier: String,
        taskSchedule: TaskSchedule,
        completionCallback: (Boolean) -> Unit
    ) {
        val taskId = runCatching {
            WorkRequestFactory.taskIdFromBGTaskIdentifier(bgTaskIdentifier, taskSchedule)
        }.getOrNull()

        if (taskId == null) {
            completionCallback(false)
            return
        }

        launch {
            try {
                val succeeded = runTask(taskId)
                completionCallback(succeeded)
            } catch (_: Throwable) {
                completionCallback(false)
            }
        }

    }

    private suspend fun runTask(id: Long): Boolean {
        val timestamp = Timestamp.now()
        val taskQueries = database.taskQueries
        val taskLogQueries = database.taskLogQueries

        val taskEntity =
            taskQueries.selectTaskByTaskId(id).executeAsOneOrNull() ?: return false

        if (taskEntity.status !is TaskStatus.Pending) {
            return false
        }

        taskQueries.updateStatus(TaskStatus.Running, timestamp, id)
        val taskId = TaskId(id)
        val task = taskEntity.toTask()
        val attemptNumber = taskEntity.runAttemptCount.toInt() + 1


        config?.telemetry?.onEvent(
            TaskTelemetryEvent.TaskStarted(
                taskId = taskId,
                task = task,
                runAttemptCount = attemptNumber
            )
        )

        val taskWorker = registry.getFactory(taskEntity.taskType)
            .create(task)

        val result: TaskResult = try {
            taskWorker.execute(taskEntity.parameters)
        } catch (error: Throwable) {
            when (error) {
                is TransientNetworkException -> TaskResult.Failure.Transient(error)
                is PermanentValidationException -> TaskResult.Failure.Permanent(error)
                else -> TaskResult.Failure.Permanent(error)
            }
        }

        taskLogQueries.insertLog(
            taskId = taskEntity.id,
            created = timestamp,
            result = result.type,
            attempt = taskEntity.runAttemptCount,
            message = null
        )

        return when (result) {
            is TaskResult.Success -> {
                val updatedNow = Timestamp.now()
                taskQueries.updateStatus(TaskStatus.Finished.Completed, updatedNow, id)

                config?.telemetry?.onEvent(
                    TaskTelemetryEvent.TaskSucceeded(
                        taskId = taskId,
                        task = task,
                        runAttemptCount = attemptNumber
                    )
                )
                true
            }

            is TaskResult.Retry -> {
                config?.telemetry?.onEvent(
                    TaskTelemetryEvent.TaskFailed(
                        taskId = taskId,
                        task = task,
                        runAttemptCount = attemptNumber,
                        error = null
                    )
                )
                false
            }

            is TaskResult.Failure.Permanent -> {
                val updatedNow = Timestamp.now()
                taskQueries.updateStatus(TaskStatus.Finished.Failed, updatedNow, id)

                config?.telemetry?.onEvent(
                    TaskTelemetryEvent.TaskFailed(
                        taskId = taskId,
                        task = task,
                        runAttemptCount = attemptNumber,
                        error = result.error
                    )
                )
                false
            }

            is TaskResult.Failure.Transient -> {
                config?.telemetry?.onEvent(
                    TaskTelemetryEvent.TaskFailed(
                        taskId = taskId,
                        task = task,
                        runAttemptCount = attemptNumber,
                        error = result.error
                    )
                )
                false
            }
        }
    }
}
