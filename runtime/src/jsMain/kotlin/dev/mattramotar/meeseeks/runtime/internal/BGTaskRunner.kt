package dev.mattramotar.meeseeks.runtime.internal

import dev.mattramotar.meeseeks.runtime.AppContext
import dev.mattramotar.meeseeks.runtime.BGTaskManagerConfig
import dev.mattramotar.meeseeks.runtime.db.MeeseeksDatabase
import dev.mattramotar.meeseeks.runtime.internal.coroutines.MeeseeksDispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

object BGTaskRunner : CoroutineScope by CoroutineScope(MeeseeksDispatchers.IO) {

    private lateinit var database: MeeseeksDatabase
    private lateinit var registry: WorkerRegistry
    private var config: BGTaskManagerConfig? = null
    private lateinit var scheduler: TaskScheduler
    private lateinit var appContext: AppContext

    private var initialized = false

    internal fun initialize(
        database: MeeseeksDatabase,
        registry: WorkerRegistry,
        config: BGTaskManagerConfig?,
        scheduler: TaskScheduler,
        context: AppContext,
    ) {
        if (initialized) return
        this.database = database
        this.registry = registry
        this.config = config
        this.scheduler = scheduler
        this.appContext = context
        initialized = true
    }

    fun run(tag: String) {
        if (!initialized) {
            console.error("BGTaskRunner called before initialization!")
            return
        }

        val taskId = WorkRequestFactory.taskIdFrom(tag)
        launch {
            executeAndChain(taskId)
        }
    }

    private suspend fun executeAndChain(taskId: String) {
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
            appContext = appContext,
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
