package dev.mattramotar.meeseeks.runtime.impl

import dev.mattramotar.meeseeks.runtime.MeeseeksRegistry
import dev.mattramotar.meeseeks.runtime.MeeseeksTelemetry
import dev.mattramotar.meeseeks.runtime.MeeseeksTelemetryEvent
import dev.mattramotar.meeseeks.runtime.MrMeeseeksId
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
    private val telemetry: MeeseeksTelemetry? = null
) : Job {

    override fun execute(context: JobExecutionContext) {
        runBlocking {
            val schedulerContext = context.scheduler.context

            val database = schedulerContext["meeseeksDatabase"] as? MeeseeksDatabase
                ?: error("MeeseeksDatabase missing from scheduler context")
            val registry = schedulerContext["meeseeksRegistry"] as? MeeseeksRegistry
                ?: error("MeeseeksRegistry missing from scheduler context")

            val taskQueries = database.taskQueries
            val taskLogQueries = database.taskLogQueries
            val taskId = context.jobDetail.jobDataMap.getLong("task_id")
            val taskEntity = taskQueries.selectTaskByTaskId(taskId).executeAsOneOrNull()
                ?: return@runBlocking

            if (taskEntity.status !is TaskStatus.Pending) {
                return@runBlocking
            }

            val timestamp = Timestamp.now()
            taskQueries.incrementRunAttemptCount(taskEntity.id)
            taskQueries.updateStatus(TaskStatus.Running, timestamp, taskEntity.id)

            val mrMeeseeksId = MrMeeseeksId(taskEntity.id)
            val task = taskEntity.toTask()
            val attemptNumber = taskEntity.runAttemptCount.toInt() + 1

            telemetry?.onEvent(
                MeeseeksTelemetryEvent.TaskStarted(
                    taskId = mrMeeseeksId,
                    task = task,
                    runAttemptCount = attemptNumber,
                )
            )

            val result: TaskResult = try {
                val meeseeksFactory = registry.getFactory(task.meeseeksType)
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
                        MeeseeksTelemetryEvent.TaskFailed(
                            taskId = mrMeeseeksId,
                            task = task,
                            error = result.error,
                            runAttemptCount = attemptNumber,
                        )
                    )
                }

                is TaskResult.Failure.Transient -> {
                    telemetry?.onEvent(
                        MeeseeksTelemetryEvent.TaskFailed(
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
                        MeeseeksTelemetryEvent.TaskFailed(
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
                        MeeseeksTelemetryEvent.TaskSucceeded(
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
