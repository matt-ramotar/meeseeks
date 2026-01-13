package dev.mattramotar.meeseeks.runtime.internal.db

import dev.mattramotar.meeseeks.runtime.*
import dev.mattramotar.meeseeks.runtime.db.TaskSpec
import dev.mattramotar.meeseeks.runtime.internal.WorkerRegistry
import dev.mattramotar.meeseeks.runtime.internal.db.model.BackoffPolicy
import dev.mattramotar.meeseeks.runtime.internal.db.model.TaskState
import dev.mattramotar.meeseeks.runtime.internal.db.model.toPublicStatus
import kotlinx.coroutines.CancellationException
import kotlin.time.Duration.Companion.milliseconds

internal class TaskRequestMappingException(
    val taskId: String,
    val payloadTypeId: String,
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)

internal object TaskMapper {

    private const val PRIORITY_LOW = 10L
    private const val PRIORITY_MEDIUM = 50L
    private const val PRIORITY_HIGH = 100L

    private const val SCHEDULE_ONE_TIME = "ONE_TIME"
    private const val SCHEDULE_PERIODIC = "PERIODIC"

    private data class UnknownPayload(val typeId: String) : TaskPayload

    data class NormalizedRequest(
        val state: TaskState,
        val payloadTypeId: String,
        val payloadData: String,
        val priority: Long,
        val requiresNetwork: Boolean,
        val requiresCharging: Boolean,
        val requiresBatteryNotLow: Boolean,
        val nextRunTimeMs: Long,
        val scheduleType: String,
        val initialDelayMs: Long,
        val intervalMs: Long,
        val flexMs: Long,
        val backoffPolicy: BackoffPolicy,
        val backoffDelayMs: Long,
        val maxRetries: Long,
        val backoffMultiplier: Double?,
        val backoffJitterFactor: Double
    )

    fun normalizeRequest(
        request: TaskRequest,
        currentTimeMs: Long,
        registry: WorkerRegistry,
        config: BGTaskManagerConfig
    ): NormalizedRequest {
        val serializedPayload = registry.serializePayload(request.payload)
        val scheduleParams = calculateScheduleParams(request.schedule)
        val nextRunTimeMs = currentTimeMs + scheduleParams.initialDelayMs
        val retryParams = calculateRetryParams(request.retryPolicy, config)

        return NormalizedRequest(
            state = TaskState.ENQUEUED,
            payloadTypeId = serializedPayload.typeId,
            payloadData = serializedPayload.data,
            priority = mapApiPriorityToDb(request.priority),
            requiresNetwork = request.preconditions.requiresNetwork,
            requiresCharging = request.preconditions.requiresCharging,
            requiresBatteryNotLow = request.preconditions.requiresBatteryNotLow,
            nextRunTimeMs = nextRunTimeMs,
            scheduleType = scheduleParams.scheduleType,
            initialDelayMs = scheduleParams.initialDelayMs,
            intervalMs = scheduleParams.intervalMs,
            flexMs = scheduleParams.flexMs,
            backoffPolicy = retryParams.backoffPolicy,
            backoffDelayMs = retryParams.backoffDelayMs,
            maxRetries = retryParams.maxRetries,
            backoffMultiplier = retryParams.backoffMultiplier,
            backoffJitterFactor = retryParams.backoffJitterFactor
        )
    }

    private data class ScheduleParams(
        val scheduleType: String,
        val initialDelayMs: Long,
        val intervalMs: Long,
        val flexMs: Long
    )

    private data class RetryParams(
        val backoffPolicy: BackoffPolicy,
        val backoffDelayMs: Long,
        val maxRetries: Long,
        val backoffMultiplier: Double?,
        val backoffJitterFactor: Double
    )

    private fun mapApiPriorityToDb(priority: TaskPriority): Long = when (priority) {
        TaskPriority.LOW -> PRIORITY_LOW
        TaskPriority.MEDIUM -> PRIORITY_MEDIUM
        TaskPriority.HIGH -> PRIORITY_HIGH
    }

    private fun mapDbPriorityToApi(priority: Long): TaskPriority = when (priority) {
        PRIORITY_LOW -> TaskPriority.LOW
        PRIORITY_HIGH -> TaskPriority.HIGH
        else -> TaskPriority.MEDIUM
    }

    private fun calculateScheduleParams(schedule: TaskSchedule): ScheduleParams {
        return when (schedule) {
            is TaskSchedule.OneTime -> ScheduleParams(
                scheduleType = SCHEDULE_ONE_TIME,
                initialDelayMs = schedule.initialDelay.inWholeMilliseconds,
                intervalMs = 0L,
                flexMs = 0L
            )
            is TaskSchedule.Periodic -> ScheduleParams(
                scheduleType = SCHEDULE_PERIODIC,
                initialDelayMs = schedule.initialDelay.inWholeMilliseconds,
                intervalMs = schedule.interval.inWholeMilliseconds,
                flexMs = schedule.flexWindow.inWholeMilliseconds
            )
        }
    }

    // Retry policy parameters calculation
    private fun calculateRetryParams(
        retryPolicy: TaskRetryPolicy,
        config: BGTaskManagerConfig
    ): RetryParams {
        val configMaxRetries = config.maxRetryCount
        return when (retryPolicy) {
            is TaskRetryPolicy.FixedInterval -> RetryParams(
                backoffPolicy = BackoffPolicy.LINEAR,
                backoffDelayMs = retryPolicy.retryInterval.inWholeMilliseconds,
                maxRetries = minOf(
                    retryPolicy.maxRetries ?: configMaxRetries,
                    configMaxRetries
                ).toLong(),
                backoffMultiplier = null,
                backoffJitterFactor = 0.0
            )
            is TaskRetryPolicy.ExponentialBackoff -> RetryParams(
                backoffPolicy = BackoffPolicy.EXPONENTIAL,
                backoffDelayMs = retryPolicy.initialInterval.inWholeMilliseconds,
                maxRetries = minOf(retryPolicy.maxRetries, configMaxRetries).toLong(),
                backoffMultiplier = retryPolicy.multiplier,
                backoffJitterFactor = retryPolicy.jitterFactor
            )
        }
    }

    fun mapToScheduledTask(entity: TaskSpec, registry: WorkerRegistry): ScheduledTask {
        return ScheduledTask(
            id = TaskId(entity.id),
            status = entity.state.toPublicStatus(),
            task = mapToTaskRequest(entity, registry),
            runAttemptCount = entity.run_attempt_count.toInt(),
            createdAt = entity.created_at_ms,
            updatedAt = entity.updated_at_ms
        )
    }

    fun mapToTaskRequest(entity: TaskSpec, registry: WorkerRegistry): TaskRequest {
        return try {
            val payload = registry.deserializePayload(entity.payload_type_id, entity.payload_data)
            val preconditions = TaskPreconditions(
                requiresNetwork = entity.requires_network,
                requiresCharging = entity.requires_charging,
                requiresBatteryNotLow = entity.requires_battery_not_low
            )
            val priority = mapDbPriorityToApi(entity.priority)
            val schedule = when (entity.schedule_type) {
                SCHEDULE_ONE_TIME -> TaskSchedule.OneTime(
                    initialDelay = entity.initial_delay_ms.milliseconds
                )
                SCHEDULE_PERIODIC -> TaskSchedule.Periodic(
                    initialDelay = entity.initial_delay_ms.milliseconds,
                    interval = entity.interval_duration_ms.milliseconds,
                    flexWindow = entity.flex_duration_ms.milliseconds
                )
                else -> error("Unknown schedule type: ${entity.schedule_type}")
            }
            val retryPolicy = when (entity.backoff_policy) {
                BackoffPolicy.LINEAR -> TaskRetryPolicy.FixedInterval(
                    retryInterval = entity.backoff_delay_ms.milliseconds,
                    maxRetries = if (entity.max_retries == Long.MAX_VALUE) null else entity.max_retries.toInt()
                )
                BackoffPolicy.EXPONENTIAL -> TaskRetryPolicy.ExponentialBackoff(
                    initialInterval = entity.backoff_delay_ms.milliseconds,
                    maxRetries = entity.max_retries.toInt(),
                    multiplier = entity.backoff_multiplier ?: 2.0,
                    jitterFactor = entity.backoff_jitter_factor
                )
            }

            TaskRequest(
                payload = payload,
                preconditions = preconditions,
                priority = priority,
                schedule = schedule,
                retryPolicy = retryPolicy
            )
        } catch (error: Throwable) {
            if (error is CancellationException) throw error
            val detail = error.message?.let { " $it" } ?: ""
            throw TaskRequestMappingException(
                taskId = entity.id,
                payloadTypeId = entity.payload_type_id,
                message = "Failed to map task request for task ${entity.id} (payload type ${entity.payload_type_id}).$detail",
                cause = error
            )
        }
    }

    fun mapToTaskRequestFallback(entity: TaskSpec): TaskRequest {
        val preconditions = TaskPreconditions(
            requiresNetwork = entity.requires_network,
            requiresCharging = entity.requires_charging,
            requiresBatteryNotLow = entity.requires_battery_not_low
        )
        val priority = mapDbPriorityToApi(entity.priority)
        val schedule = when (entity.schedule_type) {
            SCHEDULE_ONE_TIME -> TaskSchedule.OneTime(
                initialDelay = entity.initial_delay_ms.milliseconds
            )
            SCHEDULE_PERIODIC -> TaskSchedule.Periodic(
                initialDelay = entity.initial_delay_ms.milliseconds,
                interval = entity.interval_duration_ms.milliseconds,
                flexWindow = entity.flex_duration_ms.milliseconds
            )
            else -> TaskSchedule.OneTime(
                initialDelay = entity.initial_delay_ms.milliseconds
            )
        }
        val retryPolicy = when (entity.backoff_policy) {
            BackoffPolicy.LINEAR -> TaskRetryPolicy.FixedInterval(
                retryInterval = entity.backoff_delay_ms.milliseconds,
                maxRetries = if (entity.max_retries == Long.MAX_VALUE) null else entity.max_retries.toInt()
            )
            BackoffPolicy.EXPONENTIAL -> TaskRetryPolicy.ExponentialBackoff(
                initialInterval = entity.backoff_delay_ms.milliseconds,
                maxRetries = entity.max_retries.toInt(),
                multiplier = entity.backoff_multiplier ?: 2.0,
                jitterFactor = entity.backoff_jitter_factor
            )
        }

        return TaskRequest(
            payload = UnknownPayload(entity.payload_type_id),
            preconditions = preconditions,
            priority = priority,
            schedule = schedule,
            retryPolicy = retryPolicy
        )
    }
}
