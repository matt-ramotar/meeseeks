package dev.mattramotar.meeseeks.runtime.impl

import dev.mattramotar.meeseeks.runtime.TaskWorkerRegistry
import dev.mattramotar.meeseeks.runtime.TaskTelemetry
import dev.mattramotar.meeseeks.runtime.TaskTelemetryEvent
import dev.mattramotar.meeseeks.runtime.TaskId
import dev.mattramotar.meeseeks.runtime.TaskResult
import dev.mattramotar.meeseeks.runtime.TaskSchedule
import dev.mattramotar.meeseeks.runtime.TaskStatus
import dev.mattramotar.meeseeks.runtime.db.MeeseeksDatabase
import dev.mattramotar.meeseeks.runtime.impl.extensions.TaskEntityExtensions.toTask
import dev.mattramotar.meeseeks.runtime.types.PermanentValidationException
import dev.mattramotar.meeseeks.runtime.types.TransientNetworkException
import kotlinx.coroutines.runBlocking
import org.quartz.DisallowConcurrentExecution
import org.quartz.Job
import org.quartz.JobExecutionContext
import java.lang.System.currentTimeMillis


@DisallowConcurrentExecution
internal class MeeseeksQuartzJob(
    private val telemetry: TaskTelemetry? = null
) : Job {

    override fun execute(context: JobExecutionContext) {
        runBlocking {
            val schedulerContext = context.scheduler.context

            val database = schedulerContext["meeseeksDatabase"] as? MeeseeksDatabase
                ?: error("MeeseeksDatabase missing from scheduler context")
            val registry = schedulerContext["meeseeksRegistry"] as? TaskWorkerRegistry
                ?: error("MeeseeksRegistry missing from scheduler context")

            val taskQueries = database.taskQueries
            val taskLogQueries = database.taskLogQueries
            val taskWorkerId = context.jobDetail.jobDataMap.getLong("task_id")
            val taskEntity = taskQueries.selectTaskByTaskId(taskWorkerId).executeAsOneOrNull()
                ?: return@runBlocking

            if (taskEntity.status !is TaskStatus.Pending) {
                return@runBlocking
            }

            val timestamp = Timestamp.now()
            taskQueries.incrementRunAttemptCount(taskEntity.id)
            taskQueries.updateStatus(TaskStatus.Running, timestamp, taskEntity.id)

            val mrMeeseeksId = TaskId(taskEntity.id)
            val task = taskEntity.toTask()
            val attemptNumber = taskEntity.runAttemptCount.toInt() + 1

            telemetry?.onEvent(
                TaskTelemetryEvent.TaskStarted(
                    taskId = mrMeeseeksId,
                    task = task,
                    runAttemptCount = attemptNumber,
                )
            )

            val result: TaskResult = try {
                val meeseeksFactory = registry.getFactory(task.taskType)
                val meeseeks = meeseeksFactory.create(task)
                meeseeks.execute(task.parameters)

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
                attempt = attemptNumber.toLong(),
                message = null
            )

            when (result) {
                is TaskResult.Failure.Permanent -> {
                    taskQueries.updateStatus(
                        TaskStatus.Finished.Failed,
                        currentTimeMillis(),
                        taskEntity.id
                    )

                    telemetry?.onEvent(
                        TaskTelemetryEvent.TaskFailed(
                            taskId = mrMeeseeksId,
                            task = task,
                            error = result.error,
                            runAttemptCount = attemptNumber,
                        )
                    )
                }

                is TaskResult.Failure.Transient -> {
                    telemetry?.onEvent(
                        TaskTelemetryEvent.TaskFailed(
                            taskId = mrMeeseeksId,
                            task = task,
                            error = result.error,
                            runAttemptCount = attemptNumber,
                        )
                    )

                    when (task.schedule) {
                        is TaskSchedule.OneTime -> {
                            // TODO: Support retry
                        }

                        is TaskSchedule.Periodic -> {
                            // Quartz will fire again
                        }
                    }

                }

                TaskResult.Retry -> {
                    telemetry?.onEvent(
                        TaskTelemetryEvent.TaskFailed(
                            taskId = mrMeeseeksId,
                            task = task,
                            error = null,
                            runAttemptCount = attemptNumber,
                        )
                    )

                    when (task.schedule) {
                        is TaskSchedule.OneTime -> {
                            // TODO: Support retry
                        }

                        is TaskSchedule.Periodic -> {
                            // Quartz will fire again
                        }
                    }
                }

                TaskResult.Success -> {

                    telemetry?.onEvent(
                        TaskTelemetryEvent.TaskSucceeded(
                            taskId = mrMeeseeksId,
                            task = task,
                            runAttemptCount = attemptNumber,
                        )
                    )

                    taskQueries.updateStatus(
                        TaskStatus.Finished.Completed,
                        currentTimeMillis(),
                        taskEntity.id
                    )
                }
            }
        }
    }
}
