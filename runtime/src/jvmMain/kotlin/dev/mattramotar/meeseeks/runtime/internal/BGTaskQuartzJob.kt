package dev.mattramotar.meeseeks.runtime.internal

import com.mchange.v2.log.MLog.config
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
            val config = schedulerContext["bgTaskManagerConfig"] as? BGTaskManagerConfig
                ?: error("BGTaskManagerConfig missing from scheduler context")

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
                is TaskExecutor.ExecutionResult.ScheduleNextActivation -> {
                    scheduleNextActivation(context.scheduler, executionResult, config, database)
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
        config: BGTaskManagerConfig,
        database: MeeseeksDatabase
    ) {
        val factory = WorkRequestFactory()

        // Create the next WorkRequest (JobDetail + Trigger) with the delay override.
        val nextWorkRequest = factory.createWorkRequest(
            result.taskId,
            result.request,
            config,
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
}