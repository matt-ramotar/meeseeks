package dev.mattramotar.meeseeks.runtime


import dev.mattramotar.meeseeks.runtime.db.MeeseeksDatabase
import dev.mattramotar.meeseeks.runtime.db.TaskEntity
import dev.mattramotar.meeseeks.runtime.internal.Timestamp
import dev.mattramotar.meeseeks.runtime.internal.WorkerRegistry
import dev.mattramotar.meeseeks.runtime.internal.coroutines.MeeseeksDispatchers
import dev.mattramotar.meeseeks.runtime.internal.extensions.TaskEntityExtensions.toTaskRequest
import dev.mattramotar.meeseeks.runtime.types.PermanentValidationException
import dev.mattramotar.meeseeks.runtime.types.TransientNetworkException
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import platform.BackgroundTasks.BGAppRefreshTaskRequest
import platform.BackgroundTasks.BGProcessingTaskRequest
import platform.BackgroundTasks.BGTaskRequest
import platform.BackgroundTasks.BGTaskScheduler
import platform.Foundation.NSDate
import platform.Foundation.dateWithTimeIntervalSinceNow


object BGTaskRunner : CoroutineScope by CoroutineScope(MeeseeksDispatchers.IO) {

    internal lateinit var database: MeeseeksDatabase
    internal lateinit var registry: WorkerRegistry
    internal var config: BGTaskManagerConfig? = null

    fun run(
        id: Long,
        completionCallback: (Boolean) -> Unit
    ) {

        launch {
            try {
                val succeeded = runTask(id)
                completionCallback(succeeded)
            } catch (_: Throwable) {
                completionCallback(false)
            }
        }
    }

    internal suspend fun runTask(id: Long): Boolean {
        val timestamp = Timestamp.now()
        val taskQueries = database.taskQueries
        val taskLogQueries = database.taskLogQueries
        val taskEntity = claimTask(id) ?: return false
        taskQueries.updateStatus(TaskStatus.Running, timestamp, id)
        val request: TaskRequest = taskEntity.toTaskRequest()
        val attemptCount: Int = taskEntity.runAttemptCount.toInt() + 1
        val taskId = TaskId(id)

        config?.telemetry?.onEvent(
            TaskTelemetryEvent.TaskStarted(
                taskId = taskId,
                task = request,
                runAttemptCount = attemptCount
            )
        )

        val worker = getWorker(taskEntity)
        val result: TaskResult = try {
            worker.run(payload = taskEntity.payload, context = RuntimeContext(attemptCount))
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
            attempt = attemptCount.toLong(),
            message = null
        )

        return when (result) {
            is TaskResult.Success -> {

                config?.telemetry?.onEvent(
                    TaskTelemetryEvent.TaskSucceeded(
                        taskId = taskId,
                        task = request,
                        runAttemptCount = attemptCount
                    )
                )

                when (request.schedule) {
                    is TaskSchedule.OneTime -> {
                        taskQueries.updateStatus(TaskStatus.Finished.Completed, Timestamp.now(), id)
                    }

                    is TaskSchedule.Periodic -> {
                        resubmitPeriodic(id, request, attemptCount)
                    }
                }
                true
            }

            is TaskResult.Retry -> {
                config?.telemetry?.onEvent(
                    TaskTelemetryEvent.TaskFailed(
                        taskId = taskId,
                        task = request,
                        runAttemptCount = attemptCount,
                        error = null
                    )
                )

                if (request.schedule is TaskSchedule.Periodic) {
                    resubmitPeriodic(id, request, attemptCount)
                }

                false
            }

            is TaskResult.Failure.Permanent -> {
                val updatedNow = Timestamp.now()
                taskQueries.updateStatus(TaskStatus.Finished.Failed, updatedNow, id)

                config?.telemetry?.onEvent(
                    TaskTelemetryEvent.TaskFailed(
                        taskId = taskId,
                        task = request,
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
                        task = request,
                        runAttemptCount = attemptCount,
                        error = result.error
                    )
                )
                if (request.schedule is TaskSchedule.Periodic) {
                    resubmitPeriodic(id, request, attemptCount)
                }
                false
            }
        }
    }

    private fun getWorker(taskEntity: TaskEntity): Worker<TaskPayload> {
        val factory = registry.getFactory(taskEntity.payload::class)
        @Suppress("UNCHECKED_CAST")
        return factory.create(EmptyAppContext()) as Worker<TaskPayload>
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun resubmitPeriodic(
        taskId: Long, request: TaskRequest, attemptCount: Int
    ) {
        val schedule = request.schedule as? TaskSchedule.Periodic ?: return

        val identifier = if (request.preconditions.requiresNetwork || request.preconditions.requiresCharging) {
            BGTaskIdentifiers.PROCESSING
        } else {
            BGTaskIdentifiers.REFRESH
        }

        val bgTaskRequest = createNextBGTaskRequest(identifier, request, schedule)
        BGTaskScheduler.sharedScheduler.submitTaskRequest(bgTaskRequest, null)

        val now = Timestamp.now()
        database.taskQueries.updateWorkRequestId(identifier, now, taskId)
        database.taskQueries.updateStatus(TaskStatus.Pending, now, taskId)

        database.taskLogQueries.insertLog(
            taskId = taskId,
            created = now,
            result = TaskResult.Type.SuccessAndScheduledNext,
            attempt = attemptCount.toLong(),
            message = "Periodic task resubmitted by BGTaskRunner."
        )
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun createNextBGTaskRequest(
        identifier: String,
        request: TaskRequest,
        periodic: TaskSchedule.Periodic
    ): BGTaskRequest {
        val requiresNetwork = request.preconditions.requiresNetwork
        val requiresCharging = request.preconditions.requiresCharging

        val bgTaskRequest: BGTaskRequest = if (requiresNetwork || requiresCharging) {
            BGProcessingTaskRequest(identifier).apply {
                requiresNetworkConnectivity = requiresNetwork
                requiresExternalPower = requiresCharging
            }
        } else {
            BGAppRefreshTaskRequest(identifier)
        }

        val earliestSeconds = periodic.interval.inWholeSeconds.toDouble()
        if (earliestSeconds > 0.0) {
            bgTaskRequest.earliestBeginDate = NSDate.dateWithTimeIntervalSinceNow(earliestSeconds)
        }

        return bgTaskRequest
    }

    private fun claimTask(id: Long): TaskEntity? {
        val timestamp = Timestamp.now()

        val rowsAffected = database.transactionWithResult {
            database.taskQueries.atomicallyClaimAndStartTask(id, timestamp)
            database.taskQueries.selectChanges().executeAsOne().toInt()
        }


        if (rowsAffected == 0) {
            return null
        }

        return database.taskQueries.selectTaskByTaskId(id).executeAsOne()
    }
}
