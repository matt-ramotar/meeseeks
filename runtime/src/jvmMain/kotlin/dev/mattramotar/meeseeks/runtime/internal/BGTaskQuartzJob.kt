package dev.mattramotar.meeseeks.runtime.internal

import dev.mattramotar.meeseeks.runtime.BGTaskManagerConfig
import dev.mattramotar.meeseeks.runtime.EmptyAppContext
import dev.mattramotar.meeseeks.runtime.telemetry.Telemetry
import dev.mattramotar.meeseeks.runtime.db.MeeseeksDatabase
import kotlinx.coroutines.runBlocking
import org.quartz.DisallowConcurrentExecution
import org.quartz.Job
import org.quartz.JobExecutionContext
import org.quartz.JobExecutionException


@DisallowConcurrentExecution
internal class BGTaskQuartzJob(
    private val telemetry: Telemetry? = null
) : Job {

    override fun execute(context: JobExecutionContext) {
        runBlocking {
            val schedulerContext = context.scheduler.context

            val database = schedulerContext["meeseeksDatabase"] as? MeeseeksDatabase
                ?: error("MeeseeksDatabase missing from scheduler context")
            val registry = schedulerContext["workerRegistry"] as? WorkerRegistry
                ?: error("WorkerRegistry missing from scheduler context")

            val taskIdLong = context.jobDetail.jobDataMap.getLong("task_id")
            if (taskIdLong <= 0) {
                throw JobExecutionException("Invalid task_id: $taskIdLong")
            }

            // Use the centralized TaskExecutor for consistent state management
            val executionResult = TaskExecutor.execute(
                taskId = taskIdLong,
                database = database,
                registry = registry,
                appContext = EmptyAppContext(),
                config = telemetry?.let { BGTaskManagerConfig(telemetry = it) },
                attemptCount = 0 // Quartz doesn't provide attempt count
            )

            // Handle the execution result for Quartz
            when (executionResult) {
                TaskExecutor.ExecutionResult.Success -> {
                    // Task completed successfully
                    // No exception thrown, job completes normally
                }

                TaskExecutor.ExecutionResult.PlatformRetry -> {
                    // Request Quartz to retry the job immediately
                    // This ensures the task state has been reset to ENQUEUED
                    throw JobExecutionException("Transient failure, requesting retry", true)
                }

                TaskExecutor.ExecutionResult.Failure -> {
                    // Permanent failure, don't request retry
                    throw JobExecutionException("Permanent task failure", false)
                }

                is TaskExecutor.ExecutionResult.PeriodicReschedule -> {
                    // For periodic tasks, Quartz handles the scheduling via triggers
                    // We just need to complete the job successfully
                    // The periodic trigger will fire again based on its schedule
                }
            }
        }
    }
}