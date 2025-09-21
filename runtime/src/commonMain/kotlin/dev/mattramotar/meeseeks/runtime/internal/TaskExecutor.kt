package dev.mattramotar.meeseeks.runtime.internal

import dev.mattramotar.meeseeks.runtime.AppContext
import dev.mattramotar.meeseeks.runtime.BGTaskManagerConfig
import dev.mattramotar.meeseeks.runtime.RuntimeContext
import dev.mattramotar.meeseeks.runtime.TaskId
import dev.mattramotar.meeseeks.runtime.TaskPayload
import dev.mattramotar.meeseeks.runtime.TaskRequest
import dev.mattramotar.meeseeks.runtime.TaskResult
import dev.mattramotar.meeseeks.runtime.TaskSchedule
import dev.mattramotar.meeseeks.runtime.Worker
import dev.mattramotar.meeseeks.runtime.db.MeeseeksDatabase
import dev.mattramotar.meeseeks.runtime.db.TaskSpec
import dev.mattramotar.meeseeks.runtime.internal.db.TaskMapper
import dev.mattramotar.meeseeks.runtime.internal.db.model.BackoffPolicy
import dev.mattramotar.meeseeks.runtime.internal.db.model.TaskState
import dev.mattramotar.meeseeks.runtime.telemetry.TelemetryEvent
import dev.mattramotar.meeseeks.runtime.telemetry.TelemetryEvent.TaskRetryDecision.RetryDecision.CIRCUIT_BREAKER_OPEN
import dev.mattramotar.meeseeks.runtime.telemetry.TelemetryEvent.TaskRetryDecision.RetryDecision.MAX_RETRIES_EXCEEDED
import dev.mattramotar.meeseeks.runtime.telemetry.TelemetryEvent.TaskRetryDecision.RetryDecision.RATE_LIMITED
import dev.mattramotar.meeseeks.runtime.telemetry.TelemetryEvent.TaskRetryDecision.RetryDecision.RETRY_SCHEDULED
import dev.mattramotar.meeseeks.runtime.types.MaxRetriesExceededException
import dev.mattramotar.meeseeks.runtime.types.PermanentValidationException
import dev.mattramotar.meeseeks.runtime.types.RetryErrorCategory
import dev.mattramotar.meeseeks.runtime.types.RetryErrorCategory.CIRCUIT_BREAKER
import dev.mattramotar.meeseeks.runtime.types.RetryErrorCategory.RATE_LIMIT
import dev.mattramotar.meeseeks.runtime.types.RetryErrorClassifier
import kotlin.math.pow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.DurationUnit

/**
 * Centralized task executor that handles state management consistently across all platforms.
 */
internal object TaskExecutor {

    /**
     * Result of task execution that platforms can use to determine their specific behavior.
     */
    sealed class ExecutionResult {
        sealed class Terminal : ExecutionResult() {
            data object Success : Terminal()
            data object Failure : Terminal()
        }

        data class ScheduleNextActivation(
            val taskId: Long,
            val request: TaskRequest,
            val delay: Duration = Duration.ZERO,
        ) : ExecutionResult()
    }

    /**
     * Execute a task with proper state management and retry handling.
     *
     * @param taskId The ID of the task to execute.
     * @param database The database instance.
     * @param registry The worker registry for deserializing and executing tasks.
     * @param appContext The platform-specific application context.
     * @param config Optional configuration including telemetry.
     * @param attemptCount Platform-provided attempt count.
     * @return [ExecutionResult] indicating how the platform should proceed.
     */
    suspend fun execute(
        taskId: Long,
        database: MeeseeksDatabase,
        registry: WorkerRegistry,
        appContext: AppContext,
        config: BGTaskManagerConfig? = null,
        attemptCount: Int = 0
    ): ExecutionResult {
        val claimed = claimTask(taskId, database) ?: return ExecutionResult.Terminal.Failure

        val taskIdObj = TaskId(taskId)
        val request = TaskMapper.mapToTaskRequest(claimed, registry)
        val effectiveAttemptCount = maxOf(attemptCount, claimed.run_attempt_count.toInt())

        config?.telemetry?.onEvent(
            TelemetryEvent.TaskStarted(
                taskId = taskIdObj,
                task = request,
                runAttemptCount = effectiveAttemptCount
            )
        )

        val result = executeWorker(request, registry, appContext, effectiveAttemptCount)

        val taskSpec = database.taskSpecQueries.selectTaskById(taskId).executeAsOneOrNull()
        val maxRetries = taskSpec?.max_retries ?: 3
        database.taskLogQueries.insertLog(
            taskId = taskId,
            created = Timestamp.now(),
            result = result.type,
            attempt = effectiveAttemptCount.toLong(),
            message = when (result) {
                is TaskResult.Failure.Transient -> {
                    val category = result.error?.let { RetryErrorClassifier.classify(it) }
                    "Transient failure [${category?.name ?: "UNKNOWN"}] (attempt $effectiveAttemptCount/$maxRetries): ${result.error?.message}"
                }

                is TaskResult.Failure.Permanent -> {
                    val category = result.error?.let { RetryErrorClassifier.classify(it) }
                    "Permanent failure [${category?.name ?: "UNKNOWN"}]: ${result.error?.message}"
                }

                TaskResult.Retry -> "Explicit retry requested (attempt $effectiveAttemptCount/$maxRetries)"
                TaskResult.Success -> "Success"
            }
        )

        return when (result) {
            is TaskResult.Success -> {
                handleSuccess(taskId, taskIdObj, request, effectiveAttemptCount, database, config)
            }

            is TaskResult.Failure.Permanent -> {
                handlePermanentFailure(taskId, taskIdObj, request, result, effectiveAttemptCount, database, config)
                ExecutionResult.Terminal.Failure
            }

            is TaskResult.Failure.Transient, TaskResult.Retry -> {
                handleTransientFailure(
                    taskId,
                    taskIdObj,
                    request,
                    result as? TaskResult.Failure,
                    effectiveAttemptCount,
                    database,
                    config
                )
            }
        }
    }

    /**
     * Atomically claim a task by transitioning it from ENQUEUED to RUNNING.
     * This ensures only one worker can execute the task at a time.
     */
    private fun claimTask(taskId: Long, database: MeeseeksDatabase): TaskSpec? {
        return database.taskSpecQueries.transactionWithResult {
            database.taskSpecQueries.atomicallyClaimAndStartTask(taskId, Timestamp.now())
            val rowsAffected = database.taskSpecQueries.selectChanges().executeAsOne().toInt()
            if (rowsAffected == 0) {
                null
            } else {
                database.taskSpecQueries.selectTaskById(taskId).executeAsOneOrNull()
            }
        }
    }

    /**
     * Execute the worker for the given task request.
     */
    private suspend fun executeWorker(
        request: TaskRequest,
        registry: WorkerRegistry,
        appContext: AppContext,
        attemptCount: Int
    ): TaskResult {
        return try {
            val factory = registry.getFactory(request.payload::class)

            @Suppress("UNCHECKED_CAST")
            val worker = factory.create(appContext) as Worker<TaskPayload>
            worker.run(request.payload, RuntimeContext(attemptCount))
        } catch (error: Throwable) {
            classifyError(error)
        }
    }

    /**
     * Classify errors to determine if they're transient or permanent.
     */
    private fun classifyError(error: Throwable): TaskResult {
        return when {
            error is PermanentValidationException -> TaskResult.Failure.Permanent(error)
            error is MaxRetriesExceededException -> TaskResult.Failure.Permanent(error)
            RetryErrorClassifier.isRetryable(error) -> TaskResult.Failure.Transient(error)
            else -> TaskResult.Failure.Permanent(error)
        }
    }

    /**
     * Handle successful task execution.
     */
    private suspend fun handleSuccess(
        taskId: Long,
        taskIdObj: TaskId,
        request: TaskRequest,
        attemptCount: Int,
        database: MeeseeksDatabase,
        config: BGTaskManagerConfig?
    ): ExecutionResult {
        return when (val schedule = request.schedule) {
            is TaskSchedule.OneTime -> {
                database.taskSpecQueries.updateState(
                    state = TaskState.SUCCEEDED,
                    updated_at_ms = Timestamp.now(),
                    id = taskId
                )

                config?.telemetry?.onEvent(
                    TelemetryEvent.TaskSucceeded(
                        taskId = taskIdObj,
                        task = request,
                        runAttemptCount = attemptCount
                    )
                )

                ExecutionResult.Terminal.Success
            }

            is TaskSchedule.Periodic -> {
                val now = Timestamp.now()
                val base = schedule.interval
                val flex = schedule.flexWindow
                val jitter = if (flex.isPositive()) (0..flex.inWholeMilliseconds).random().milliseconds else Duration.ZERO
                val delay = (base - flex + jitter).coerceAtLeast(Duration.ZERO)

                database.taskSpecQueries.updateStateAndNextRunTime(
                    state = TaskState.ENQUEUED,
                    updated_at_ms = now,
                    next_run_time_ms = now + delay.inWholeMilliseconds,
                    id = taskId
                )

                config?.telemetry?.onEvent(
                    TelemetryEvent.TaskSucceeded(
                        taskId = taskIdObj,
                        task = request,
                        runAttemptCount = attemptCount
                    )
                )

                ExecutionResult.ScheduleNextActivation(taskId, request, delay)
            }
        }
    }

    /**
     * Handle permanent task failure.
     */
    private suspend fun handlePermanentFailure(
        taskId: Long,
        taskIdObj: TaskId,
        request: TaskRequest,
        result: TaskResult.Failure.Permanent,
        attemptCount: Int,
        database: MeeseeksDatabase,
        config: BGTaskManagerConfig?
    ) {
        database.taskSpecQueries.updateState(
            state = TaskState.FAILED,
            updated_at_ms = Timestamp.now(),
            id = taskId
        )

        config?.telemetry?.onEvent(
            TelemetryEvent.TaskFailed(
                taskId = taskIdObj,
                task = request,
                error = result.error,
                runAttemptCount = attemptCount
            )
        )
    }

    /**
     * Handle transient failure or retry request.
     */
    private suspend fun handleTransientFailure(
        taskId: Long,
        taskIdObj: TaskId,
        request: TaskRequest,
        result: TaskResult.Failure?,
        attemptCount: Int,
        database: MeeseeksDatabase,
        config: BGTaskManagerConfig?
    ): ExecutionResult {
        val taskSpec = database.taskSpecQueries.selectTaskById(taskId).executeAsOneOrNull()
        val maxRetries = taskSpec?.max_retries?.toInt() ?: 3

        if (attemptCount >= maxRetries) {
            val exceededException = MaxRetriesExceededException(
                taskId = taskId,
                attemptNumber = attemptCount,
                maxRetries = maxRetries,
                cause = result?.error
            )

            handlePermanentFailure(
                taskId,
                taskIdObj,
                request,
                TaskResult.Failure.Permanent(exceededException),
                attemptCount,
                database,
                config
            )
            return ExecutionResult.Terminal.Failure
        }

        val nextRetryDelayMs = calculateRetryDelay(taskSpec, result, attemptCount)

        database.taskSpecQueries.transaction {
            database.taskSpecQueries.updateStateAndNextRunTime(
                state = TaskState.ENQUEUED,
                updated_at_ms = Timestamp.now(),
                next_run_time_ms = nextRetryDelayMs,
                id = taskId
            )
        }

        config?.telemetry?.let { telemetry ->
            telemetry.onEvent(
                TelemetryEvent.TaskFailed(
                    taskId = taskIdObj,
                    task = request,
                    error = result?.error,
                    runAttemptCount = attemptCount
                )
            )

            val errorCategory = result?.error?.let { RetryErrorClassifier.classify(it) }
            telemetry.onEvent(
                TelemetryEvent.TaskRetryScheduled(
                    taskId = taskIdObj,
                    task = request,
                    attemptCount = attemptCount,
                    nextRetryDelayMs = nextRetryDelayMs,
                    remainingRetries = maxRetries - attemptCount,
                    errorCategory = errorCategory,
                    errorMessage = result?.error?.message,
                    backoffPolicy = taskSpec?.backoff_policy
                )
            )

            val decision = when {
                attemptCount >= maxRetries -> MAX_RETRIES_EXCEEDED
                errorCategory == CIRCUIT_BREAKER -> CIRCUIT_BREAKER_OPEN
                errorCategory == RATE_LIMIT -> RATE_LIMITED
                else -> RETRY_SCHEDULED
            }

            telemetry.onEvent(
                TelemetryEvent.TaskRetryDecision(
                    taskId = taskIdObj,
                    decision = decision,
                    attemptCount = attemptCount,
                    maxRetries = maxRetries,
                    errorCategory = errorCategory ?: RetryErrorCategory.UNKNOWN,
                    reason = result?.error?.message ?: "Retry requested"
                )
            )
        }

        return ExecutionResult.ScheduleNextActivation(taskId, request, nextRetryDelayMs.milliseconds)
    }

    /**
     * Calculate retry delay based on backoff policy and error type.
     */
    private fun calculateRetryDelay(
        taskSpec: TaskSpec?,
        result: TaskResult?,
        attemptCount: Int
    ): Long {
        val backoffPolicy = taskSpec?.backoff_policy
            ?: BackoffPolicy.EXPONENTIAL
        val baseDelayMs = taskSpec?.backoff_delay_ms ?: 1000L
        val multiplier = taskSpec?.backoff_multiplier ?: 2.0

        val error = when (result) {
            is TaskResult.Failure -> result.error
            else -> null
        }

        val suggestedDelay = error?.let {
            RetryErrorClassifier.suggestedRetryDelay(it, baseDelayMs)
        } ?: baseDelayMs

        return when (backoffPolicy) {
            BackoffPolicy.LINEAR -> {
                suggestedDelay * attemptCount
            }

            BackoffPolicy.EXPONENTIAL -> {
                (suggestedDelay * multiplier.pow((attemptCount - 1).toDouble())).toLong()
                    .coerceAtMost(5.minutes.toLong(DurationUnit.MILLISECONDS))
            }
        }
    }
}