package dev.mattramotar.meeseeks.runtime


import dev.mattramotar.meeseeks.runtime.db.MeeseeksDatabase
import dev.mattramotar.meeseeks.runtime.db.TaskEntity
import dev.mattramotar.meeseeks.runtime.impl.Timestamp
import dev.mattramotar.meeseeks.runtime.impl.WorkRequestFactory
import dev.mattramotar.meeseeks.runtime.impl.WorkerRegistry
import dev.mattramotar.meeseeks.runtime.impl.coroutines.MeeseeksDispatchers
import dev.mattramotar.meeseeks.runtime.impl.extensions.TaskEntityExtensions.toTaskRequest
import dev.mattramotar.meeseeks.runtime.types.PermanentValidationException
import dev.mattramotar.meeseeks.runtime.types.TransientNetworkException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch


object BGTaskRunner : CoroutineScope by CoroutineScope(MeeseeksDispatchers.IO) {

    internal lateinit var database: MeeseeksDatabase
    internal lateinit var registry: WorkerRegistry
    internal var config: BGTaskManagerConfig? = null


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
        val task = taskEntity.toTaskRequest()
        val attemptCount = taskEntity.runAttemptCount.toInt() + 1


        config?.telemetry?.onEvent(
            TaskTelemetryEvent.TaskStarted(
                taskId = taskId,
                task = task,
                runAttemptCount = attemptCount
            )
        )

        val worker = getWorker(taskEntity)
        val result: TaskResult = try {
            worker.run(data = taskEntity.dynamicData, context = RuntimeContext(attemptCount))
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
                        runAttemptCount = attemptCount
                    )
                )
                true
            }

            is TaskResult.Retry -> {
                config?.telemetry?.onEvent(
                    TaskTelemetryEvent.TaskFailed(
                        taskId = taskId,
                        task = task,
                        runAttemptCount = attemptCount,
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
                        runAttemptCount = attemptCount,
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
                        runAttemptCount = attemptCount,
                        error = result.error
                    )
                )
                false
            }
        }
    }

    private fun getWorker(taskEntity: TaskEntity): Worker<DynamicData> {
        val factory = registry.getFactory(taskEntity.dynamicData::class)
        @Suppress("UNCHECKED_CAST")
        return factory.create(EmptyAppContext()) as Worker<DynamicData>
    }
}
