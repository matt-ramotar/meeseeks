package dev.mattramotar.meeseeks.runtime.internal

import dev.mattramotar.meeseeks.runtime.db.MeeseeksDatabase
import kotlinx.coroutines.runBlocking
import org.quartz.DisallowConcurrentExecution
import org.quartz.Job
import org.quartz.JobExecutionContext
import org.quartz.JobExecutionException


@DisallowConcurrentExecution
internal class BGTaskQuartzJob : Job {

    override fun execute(context: JobExecutionContext) {
        runBlocking {
            val schedulerContext = context.scheduler.context

            val dependencies = schedulerContext[CTX_MEESEEKS_DEPS] as? MeeseeksDependencies
                ?: error("MeeseeksDependencies missing from scheduler context.")

            val taskIdFromString = context.jobDetail.jobDataMap.getString("task_id")
            val taskId = taskIdFromString
                ?: context.jobDetail.jobDataMap.getLong("task_id")
                    .takeIf { it > 0 }
                    ?.toString()
            if (taskId.isNullOrBlank()) {
                throw JobExecutionException("Invalid task_id: $taskId")
            }

            // Use the centralized TaskExecutor for consistent state management
            val executionResult = TaskExecutor.execute(
                taskId = taskId,
                database = dependencies.database,
                registry = dependencies.registry,
                appContext = dependencies.appContext,
                config = dependencies.config,
                attemptCount = 0 // Quartz doesn't provide attempt count
            )

            // Handle the execution result for Quartz
            when (executionResult) {
                is TaskExecutor.ExecutionResult.ScheduleNextActivation -> {
                    scheduleNextActivation(context.scheduler, executionResult, dependencies.database)
                }

                TaskExecutor.ExecutionResult.Terminal.Failure,
                TaskExecutor.ExecutionResult.Terminal.Success -> {
                    // Job completes normally.
                    // We are not throwing JobExecutionException(refireImmediately = true) because
                    // Meeseeks is the single SOT for scheduling.
                }
            }
        }
    }


    private fun scheduleNextActivation(
        scheduler: org.quartz.Scheduler,
        result: TaskExecutor.ExecutionResult.ScheduleNextActivation,
        database: MeeseeksDatabase
    ) {
        val factory = WorkRequestFactory()

        // Create the next WorkRequest (JobDetail + Trigger) with the delay override.
        val nextWorkRequest = factory.createWorkRequest(
            result.taskId,
            result.request,
            delayOverrideMs = result.delay.inWholeMilliseconds
        )

        // Schedule the new activation.
        // We are using scheduleJob with replace set to true to ensure the JobDetail exists (if durable) and the new trigger is associated.
        scheduler.scheduleJob(
            nextWorkRequest.jobDetail,
            setOf(nextWorkRequest.triggers.first()),
            true // Replace the existing job definition if needed
        )

        // Update platform_id in DB if tracking specific trigger IDs is required.
        database.taskSpecQueries.updatePlatformId(
            platform_id = nextWorkRequest.id,
            updated_at_ms = Timestamp.now(),
            id = result.taskId
        )
    }

    companion object {
        const val CTX_MEESEEKS_DEPS = "meeseeksDependencies"
    }
}
