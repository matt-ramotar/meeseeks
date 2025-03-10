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


object MeeseeksBGTaskRunner: CoroutineScope by CoroutineScope(MeeseeksDispatchers.IO) {

    internal lateinit var database: MeeseeksDatabase
    internal lateinit var registry: MeeseeksRegistry
    internal var config: MeeseeksBoxConfig? = null


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
                val succeeded = runMeeseeksTask(taskId)
                completionCallback(succeeded)
            } catch (_: Throwable) {
                completionCallback(false)
            }
        }

    }

    private suspend fun runMeeseeksTask(taskId: Long): Boolean {
        val timestamp = Timestamp.now()
        val taskQueries = database.taskQueries
        val taskLogQueries = database.taskLogQueries

        val taskEntity =
            taskQueries.selectTaskByTaskId(taskId).executeAsOneOrNull() ?: return false

        if (taskEntity.status !is TaskStatus.Pending) {
            return false
        }

        taskQueries.updateStatus(TaskStatus.Running, timestamp, taskId)
        val mrMeeseeksId = MrMeeseeksId(taskId)
        val task = taskEntity.toTask()
        val attemptNumber = taskEntity.runAttemptCount.toInt() + 1


        config?.telemetry?.onEvent(
            MeeseeksTelemetryEvent.TaskStarted(
                taskId = mrMeeseeksId,
                task = task,
                runAttemptCount = attemptNumber
            )
        )

        val meeseeks = registry.getFactory(taskEntity.meeseeksType)
            .create(task)

        val result: TaskResult = try {
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
            created = timestamp,
            result = result.type,
            attempt = taskEntity.runAttemptCount,
            message = null
        )

        return when (result) {
            is TaskResult.Success -> {
                val updatedNow = Timestamp.now()
                taskQueries.updateStatus(TaskStatus.Finished.Completed, updatedNow, taskId)

                config?.telemetry?.onEvent(
                    MeeseeksTelemetryEvent.TaskSucceeded(
                        taskId = mrMeeseeksId,
                        task = task,
                        runAttemptCount = attemptNumber
                    )
                )
                true
            }

            is TaskResult.Retry -> {
                config?.telemetry?.onEvent(
                    MeeseeksTelemetryEvent.TaskFailed(
                        taskId = mrMeeseeksId,
                        task = task,
                        runAttemptCount = attemptNumber,
                        error = null
                    )
                )
                false
            }

            is TaskResult.Failure.Permanent -> {
                val updatedNow = Timestamp.now()
                taskQueries.updateStatus(TaskStatus.Finished.Cancelled, updatedNow, taskId)

                config?.telemetry?.onEvent(
                    MeeseeksTelemetryEvent.TaskFailed(
                        taskId = mrMeeseeksId,
                        task = task,
                        runAttemptCount = attemptNumber,
                        error = result.error
                    )
                )
                false
            }

            is TaskResult.Failure.Transient -> {
                config?.telemetry?.onEvent(
                    MeeseeksTelemetryEvent.TaskFailed(
                        taskId = mrMeeseeksId,
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
