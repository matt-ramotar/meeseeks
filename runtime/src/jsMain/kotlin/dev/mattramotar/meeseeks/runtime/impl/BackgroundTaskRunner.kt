package dev.mattramotar.meeseeks.runtime.impl

import dev.mattramotar.meeseeks.runtime.BackgroundTaskConfig
import dev.mattramotar.meeseeks.runtime.DynamicData
import dev.mattramotar.meeseeks.runtime.EmptyAppContext
import dev.mattramotar.meeseeks.runtime.RuntimeContext
import dev.mattramotar.meeseeks.runtime.TaskId
import dev.mattramotar.meeseeks.runtime.TaskRequest
import dev.mattramotar.meeseeks.runtime.TaskResult
import dev.mattramotar.meeseeks.runtime.TaskStatus
import dev.mattramotar.meeseeks.runtime.TaskTelemetryEvent
import dev.mattramotar.meeseeks.runtime.Worker
import dev.mattramotar.meeseeks.runtime.db.MeeseeksDatabase
import dev.mattramotar.meeseeks.runtime.db.TaskEntity
import dev.mattramotar.meeseeks.runtime.impl.coroutines.MeeseeksDispatchers
import dev.mattramotar.meeseeks.runtime.impl.extensions.TaskEntityExtensions.toTaskRequest
import dev.mattramotar.meeseeks.runtime.types.PermanentValidationException
import dev.mattramotar.meeseeks.runtime.types.TransientNetworkException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

object BackgroundTaskRunner : CoroutineScope by CoroutineScope(MeeseeksDispatchers.IO) {

    internal lateinit var database: MeeseeksDatabase
    internal lateinit var registry: WorkerRegistry
    internal var config: BackgroundTaskConfig? = null

    fun run(
        tag: String
    ) {
        val taskId = WorkRequestFactory.taskIdFrom(tag)
        val taskEntity =
            database.taskQueries.selectTaskByTaskId(taskId).executeAsOneOrNull() ?: return
        launch {
            runTask(taskEntity)
        }

    }

    private suspend fun runTask(taskEntity: TaskEntity) {
        val taskQueries = database.taskQueries
        val taskLogQueries = database.taskLogQueries

        if (taskEntity.status !is TaskStatus.Pending) {
            console.log("Task id=${taskEntity.id} not pending, skipping")
            return
        }

        val taskEntityId = taskEntity.id
        val taskId = TaskId(taskEntityId)
        val request = taskEntity.toTaskRequest()
        val attemptCount = taskEntity.runAttemptCount.toInt() + 1

        val timestamp = Timestamp.now()
        taskQueries.updateStatus(TaskStatus.Running, timestamp, taskEntityId)

        config?.telemetry?.onEvent(
            TaskTelemetryEvent.TaskStarted(taskId, request, attemptCount)
        )

        val worker = getWorker(request, registry)
        val result: TaskResult = try {
            worker.run(request.data, RuntimeContext(attemptCount))
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
                taskQueries.updateStatus(TaskStatus.Finished.Completed, Timestamp.now(), taskEntityId)
                config?.telemetry?.onEvent(
                    TaskTelemetryEvent.TaskSucceeded(taskId, request, attemptCount)
                )
            }

            is TaskResult.Failure.Transient,
            is TaskResult.Retry -> {
                config?.telemetry?.onEvent(
                    TaskTelemetryEvent.TaskFailed(
                        taskId,
                        request,
                        (result as? TaskResult.Failure)?.error,
                        attemptCount
                    )
                )
            }

            is TaskResult.Failure.Permanent -> {
                taskQueries.updateStatus(TaskStatus.Finished.Failed, Timestamp.now(), taskEntityId)
                config?.telemetry?.onEvent(
                    TaskTelemetryEvent.TaskFailed(
                        taskId,
                        request,
                        result.error,
                        attemptCount
                    )
                )
            }
        }
    }

    private fun getWorker(request: TaskRequest, registry: WorkerRegistry): Worker<DynamicData> {
        val factory = registry.getFactory(request.data::class)
        @Suppress("UNCHECKED_CAST")
        return factory.create(EmptyAppContext()) as Worker<DynamicData>
    }
}