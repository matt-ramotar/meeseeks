package dev.mattramotar.meeseeks.runtime.impl

import dev.mattramotar.meeseeks.runtime.DynamicData
import dev.mattramotar.meeseeks.runtime.EmptyAppContext
import dev.mattramotar.meeseeks.runtime.RuntimeContext
import dev.mattramotar.meeseeks.runtime.TaskTelemetry
import dev.mattramotar.meeseeks.runtime.TaskTelemetryEvent
import dev.mattramotar.meeseeks.runtime.TaskId
import dev.mattramotar.meeseeks.runtime.TaskRequest
import dev.mattramotar.meeseeks.runtime.TaskResult
import dev.mattramotar.meeseeks.runtime.TaskSchedule
import dev.mattramotar.meeseeks.runtime.TaskStatus
import dev.mattramotar.meeseeks.runtime.Worker
import dev.mattramotar.meeseeks.runtime.db.MeeseeksDatabase
import dev.mattramotar.meeseeks.runtime.impl.extensions.TaskEntityExtensions.toTaskRequest
import dev.mattramotar.meeseeks.runtime.types.PermanentValidationException
import dev.mattramotar.meeseeks.runtime.types.TransientNetworkException
import kotlinx.coroutines.runBlocking
import org.quartz.DisallowConcurrentExecution
import org.quartz.Job
import org.quartz.JobExecutionContext
import java.lang.System.currentTimeMillis


@DisallowConcurrentExecution
internal class BGTaskQuartzJob(
    private val telemetry: TaskTelemetry? = null
) : Job {

    override fun execute(context: JobExecutionContext) {
        runBlocking {
            val schedulerContext = context.scheduler.context

            val database = schedulerContext["meeseeksDatabase"] as? MeeseeksDatabase
                ?: error("MeeseeksDatabase missing from scheduler context")
            val registry = schedulerContext["workerRegistry"] as? WorkerRegistry
                ?: error("WorkerRegistry missing from scheduler context")

            val taskQueries = database.taskQueries
            val taskLogQueries = database.taskLogQueries
            val taskEntityId = context.jobDetail.jobDataMap.getLong("task_id")
            val taskEntity = taskQueries.selectTaskByTaskId(taskEntityId).executeAsOneOrNull()
                ?: return@runBlocking

            if (taskEntity.status !is TaskStatus.Pending) {
                return@runBlocking
            }

            val timestamp = Timestamp.now()
            taskQueries.incrementRunAttemptCount(taskEntity.id)
            taskQueries.updateStatus(TaskStatus.Running, timestamp, taskEntity.id)

            val taskId = TaskId(taskEntity.id)
            val request = taskEntity.toTaskRequest()
            val attemptCount = taskEntity.runAttemptCount.toInt() + 1

            telemetry?.onEvent(
                TaskTelemetryEvent.TaskStarted(
                    taskId = taskId,
                    task = request,
                    runAttemptCount = attemptCount,
                )
            )

            val result: TaskResult = try {
                val worker = getWorker(request, registry)
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
                attempt = attemptCount.toLong(),
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
                            taskId = taskId,
                            task = request,
                            error = result.error,
                            runAttemptCount = attemptCount,
                        )
                    )
                }

                is TaskResult.Failure.Transient -> {
                    telemetry?.onEvent(
                        TaskTelemetryEvent.TaskFailed(
                            taskId = taskId,
                            task = request,
                            error = result.error,
                            runAttemptCount = attemptCount,
                        )
                    )

                    when (request.schedule) {
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
                            taskId = taskId,
                            task = request,
                            error = null,
                            runAttemptCount = attemptCount,
                        )
                    )

                    when (request.schedule) {
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
                            taskId = taskId,
                            task = request,
                            runAttemptCount = attemptCount,
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

    private fun getWorker(request: TaskRequest, registry: WorkerRegistry): Worker<DynamicData> {
        val factory = registry.getFactory(request.data::class)
        @Suppress("UNCHECKED_CAST")
        return factory.create(EmptyAppContext()) as Worker<DynamicData>
    }
}
