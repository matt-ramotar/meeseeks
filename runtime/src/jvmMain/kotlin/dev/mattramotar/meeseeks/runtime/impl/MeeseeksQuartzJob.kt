package dev.mattramotar.meeseeks.runtime.impl

import dev.mattramotar.meeseeks.runtime.MeeseeksRegistry
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
internal class MeeseeksQuartzJob : Job {

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

            val task = taskEntity.toTask()
            val attemptNumber = taskEntity.runAttemptCount + 1
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
                        TaskStatus.Finished.Cancelled,
                        currentTimeMillis(),
                        taskEntity.id
                    )
                }

                is TaskResult.Failure.Transient,
                TaskResult.Retry -> {
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
