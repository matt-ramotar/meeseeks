package dev.mattramotar.meeseeks.runtime.internal

import dev.mattramotar.meeseeks.runtime.BGTaskManagerConfig
import dev.mattramotar.meeseeks.runtime.TaskPayload
import dev.mattramotar.meeseeks.runtime.EmptyAppContext
import dev.mattramotar.meeseeks.runtime.RuntimeContext
import dev.mattramotar.meeseeks.runtime.TaskId
import dev.mattramotar.meeseeks.runtime.TaskRequest
import dev.mattramotar.meeseeks.runtime.TaskResult
import dev.mattramotar.meeseeks.runtime.TaskTelemetryEvent
import dev.mattramotar.meeseeks.runtime.Worker
import dev.mattramotar.meeseeks.runtime.db.MeeseeksDatabase
import dev.mattramotar.meeseeks.runtime.db.TaskSpec
import dev.mattramotar.meeseeks.runtime.internal.coroutines.MeeseeksDispatchers
import dev.mattramotar.meeseeks.runtime.internal.db.TaskMapper
import dev.mattramotar.meeseeks.runtime.internal.db.model.TaskState
import dev.mattramotar.meeseeks.runtime.internal.db.model.toPublicStatus
import dev.mattramotar.meeseeks.runtime.types.PermanentValidationException
import dev.mattramotar.meeseeks.runtime.types.TransientNetworkException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

object BGTaskRunner : CoroutineScope by CoroutineScope(MeeseeksDispatchers.IO) {

    internal lateinit var database: MeeseeksDatabase
    internal lateinit var registry: WorkerRegistry
    internal var config: BGTaskManagerConfig? = null

    fun run(
        tag: String
    ) {
        val taskId = WorkRequestFactory.taskIdFrom(tag)
        val taskSpec =
            database.taskSpecQueries.selectTaskById(taskId).executeAsOneOrNull() ?: return
        launch {
            runTask(taskSpec)
        }

    }

    private suspend fun runTask(taskSpec: TaskSpec) {
        val taskSpecQueries = database.taskSpecQueries
        val taskLogQueries = database.taskLogQueries

        val currentStatus = taskSpec.state.toPublicStatus()
        if (currentStatus !is dev.mattramotar.meeseeks.runtime.TaskStatus.Pending) {
            console.log("Task id=${taskSpec.id} not pending, skipping")
            return
        }

        val taskSpecId = taskSpec.id
        val taskId = TaskId(taskSpecId)
        val request = TaskMapper.mapToTaskRequest(taskSpec, registry)
        val attemptCount = taskSpec.run_attempt_count.toInt() + 1

        val timestamp = Timestamp.now()
        taskSpecQueries.updateState(TaskState.RUNNING, timestamp, taskSpecId)

        config?.telemetry?.onEvent(
            TaskTelemetryEvent.TaskStarted(taskId, request, attemptCount)
        )

        val worker = getWorker(request, registry)
        val result: TaskResult = try {
            worker.run(request.payload, RuntimeContext(attemptCount))
        } catch (error: Throwable) {

            when (error) {
                is TransientNetworkException -> TaskResult.Failure.Transient(error)
                is PermanentValidationException -> TaskResult.Failure.Permanent(error)
                else -> TaskResult.Failure.Permanent(error)
            }
        }

        taskLogQueries.insertLog(
            taskId = taskSpec.id,
            created = timestamp,
            result = result.type,
            attempt = taskSpec.run_attempt_count,
            message = null
        )

        when (result) {
            is TaskResult.Success -> {
                taskSpecQueries.updateState(TaskState.SUCCEEDED, Timestamp.now(), taskSpecId)
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
                taskSpecQueries.updateState(TaskState.FAILED, Timestamp.now(), taskSpecId)
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

    private fun getWorker(request: TaskRequest, registry: WorkerRegistry): Worker<TaskPayload> {
        val factory = registry.getFactory(request.payload::class)
        @Suppress("UNCHECKED_CAST")
        return factory.create(EmptyAppContext()) as Worker<TaskPayload>
    }
}