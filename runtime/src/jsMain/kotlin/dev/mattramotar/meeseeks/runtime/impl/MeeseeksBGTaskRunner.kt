package dev.mattramotar.meeseeks.runtime.impl

import dev.mattramotar.meeseeks.runtime.MeeseeksBoxConfig
import dev.mattramotar.meeseeks.runtime.MeeseeksRegistry
import dev.mattramotar.meeseeks.runtime.MeeseeksTelemetryEvent
import dev.mattramotar.meeseeks.runtime.MrMeeseeksId
import dev.mattramotar.meeseeks.runtime.TaskResult
import dev.mattramotar.meeseeks.runtime.TaskStatus
import dev.mattramotar.meeseeks.runtime.db.MeeseeksDatabase
import dev.mattramotar.meeseeks.runtime.db.TaskEntity
import dev.mattramotar.meeseeks.runtime.impl.coroutines.MeeseeksDispatchers
import dev.mattramotar.meeseeks.runtime.impl.extensions.TaskEntityExtensions.toTask
import dev.mattramotar.meeseeks.runtime.types.PermanentValidationException
import dev.mattramotar.meeseeks.runtime.types.TransientNetworkException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

object MeeseeksBGTaskRunner : CoroutineScope by CoroutineScope(MeeseeksDispatchers.IO) {

    internal lateinit var database: MeeseeksDatabase
    internal lateinit var registry: MeeseeksRegistry
    internal var config: MeeseeksBoxConfig? = null

    fun run(
        tag: String
    ) {
        val taskId = WorkRequestFactory.taskIdFrom(tag)
        val taskEntity =
            database.taskQueries.selectTaskByTaskId(taskId).executeAsOneOrNull() ?: return
        launch {
            runMeeseeksTask(taskEntity)
        }

    }

    private suspend fun runMeeseeksTask(taskEntity: TaskEntity) {
        val taskQueries = database.taskQueries
        val taskLogQueries = database.taskLogQueries

        if (taskEntity.status !is TaskStatus.Pending) {
            console.log("Task id=${taskEntity.id} not pending, skipping")
            return
        }

        val taskId = taskEntity.id
        val mrMeeseeksId = MrMeeseeksId(taskId)
        val task = taskEntity.toTask()
        val attemptNumber = taskEntity.runAttemptCount.toInt() + 1

        val timestamp = Timestamp.now()
        taskQueries.updateStatus(TaskStatus.Running, timestamp, taskId)

        config?.telemetry?.onEvent(
            MeeseeksTelemetryEvent.TaskStarted(mrMeeseeksId, task, attemptNumber)
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

        when (result) {
            is TaskResult.Success -> {
                taskQueries.updateStatus(TaskStatus.Finished.Completed, Timestamp.now(), taskId)
                config?.telemetry?.onEvent(
                    MeeseeksTelemetryEvent.TaskSucceeded(mrMeeseeksId, task, attemptNumber)
                )
            }

            is TaskResult.Failure.Transient,
            is TaskResult.Retry -> {
                config?.telemetry?.onEvent(
                    MeeseeksTelemetryEvent.TaskFailed(
                        mrMeeseeksId,
                        task,
                        (result as? TaskResult.Failure)?.error,
                        attemptNumber
                    )
                )
            }

            is TaskResult.Failure.Permanent -> {
                taskQueries.updateStatus(TaskStatus.Finished.Cancelled, Timestamp.now(), taskId)
                config?.telemetry?.onEvent(
                    MeeseeksTelemetryEvent.TaskFailed(
                        mrMeeseeksId,
                        task,
                        result.error,
                        attemptNumber
                    )
                )
            }
        }
    }
}