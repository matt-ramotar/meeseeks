package dev.mattramotar.meeseeks.runtime.internal

import dev.mattramotar.meeseeks.runtime.BGTaskManagerConfig
import dev.mattramotar.meeseeks.runtime.EmptyAppContext
import dev.mattramotar.meeseeks.runtime.db.MeeseeksDatabase
import dev.mattramotar.meeseeks.runtime.internal.coroutines.MeeseeksDispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

object BGTaskRunner : CoroutineScope by CoroutineScope(MeeseeksDispatchers.IO) {

    internal lateinit var database: MeeseeksDatabase
    internal lateinit var registry: WorkerRegistry
    internal var config: BGTaskManagerConfig? = null
    private lateinit var scheduler: TaskScheduler

    internal fun initialize(
        db: MeeseeksDatabase,
        registry: WorkerRegistry,
        config: BGTaskManagerConfig?,
        scheduler: TaskScheduler
    ) {
        this.database = db
        this.registry = registry
        this.config = config
        this.scheduler = scheduler
    }

    fun run(tag: String) {
        val taskId = WorkRequestFactory.taskIdFrom(tag)
        launch {
            executeAndChain(taskId)
        }
    }

    private suspend fun executeAndChain(taskId: Long) {
        scheduler.clearFallbackTimer(taskId)

        // Fetch attempt count from DB (JS platform doesn't reliably track it).
        val taskSpec = database.taskSpecQueries.selectTaskById(taskId).executeAsOneOrNull()

        if (taskSpec == null) {
            console.warn("Task $taskId not found in DB. Stopping execution.")
            scheduler.removeScheduledTask(taskId)
            return
        }

        val attemptCount = taskSpec.run_attempt_count.toInt()

        val executionResult = TaskExecutor.execute(
            taskId = taskId,
            database = database,
            registry = registry,
            appContext = EmptyAppContext(),
            config = config,
            attemptCount = attemptCount
        )

        when (executionResult) {
            is TaskExecutor.ExecutionResult.Terminal.Failure -> {
                console.log("Task $taskId is terminal failure.")
                scheduler.removeScheduledTask(taskId)
            }

            is TaskExecutor.ExecutionResult.Terminal.Success -> {
                console.log("Task $taskId is terminal success.")
                scheduler.removeScheduledTask(taskId)
            }

            is TaskExecutor.ExecutionResult.ScheduleNextActivation -> {
                console.log("Task $taskId requires next activation (Retry or Periodic) in ${executionResult.delay.inWholeMilliseconds}ms.")
                scheduleNextActivation(executionResult)
            }
        }
    }

    private fun scheduleNextActivation(result: TaskExecutor.ExecutionResult.ScheduleNextActivation) {
        // Unified scheduling logic in TaskScheduler will decide between SyncManager or setTimeout.
        scheduler.scheduleActivation(
            result.taskId,
            result.request.preconditions,
            result.delay.inWholeMilliseconds
        )
    }
}