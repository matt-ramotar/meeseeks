package dev.mattramotar.meeseeks.runtime.telemetry

import dev.mattramotar.meeseeks.runtime.TaskId
import dev.mattramotar.meeseeks.runtime.TaskRequest
import dev.mattramotar.meeseeks.runtime.internal.Timestamp
import dev.mattramotar.meeseeks.runtime.internal.db.model.BackoffPolicy
import dev.mattramotar.meeseeks.runtime.types.RetryErrorCategory
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

sealed class TelemetryEvent {

    abstract fun structured(): String

    data class TaskScheduled(
        val taskId: TaskId,
        val task: TaskRequest
    ) : TelemetryEvent() {
        override fun structured(): String {
            val log = mapOf(
                "event" to "TASK_SCHEDULED",
                "timestamp" to Timestamp.now(),
                "taskId" to taskId.value,
                "taskType" to (task.payload::class.simpleName ?: "Unknown"),
                "priority" to task.priority.name,
                "requiresNetwork" to task.preconditions.requiresNetwork,
                "requiresCharging" to task.preconditions.requiresCharging
            )
            return Json.encodeToString(log)
        }
    }

    data class TaskStarted(
        val taskId: TaskId,
        val task: TaskRequest,
        val runAttemptCount: Int = 1,
    ) : TelemetryEvent() {
        override fun structured(): String {
            val log = mapOf(
                "event" to "TASK_STARTED",
                "timestamp" to Timestamp.now(),
                "taskId" to taskId.value,
                "taskType" to (task.payload::class.simpleName ?: "Unknown"),
                "runAttemptCount" to runAttemptCount,
                "priority" to task.priority.name,
                "requiresNetwork" to task.preconditions.requiresNetwork,
                "requiresCharging" to task.preconditions.requiresCharging
            )
            return Json.encodeToString(log)
        }
    }

    data class TaskSucceeded(
        val taskId: TaskId,
        val task: TaskRequest,
        val runAttemptCount: Int = 1,
    ) : TelemetryEvent() {
        override fun structured(): String {
            val log = mapOf(
                "event" to "TASK_SUCCEEDED",
                "timestamp" to Timestamp.now(),
                "taskId" to taskId.value,
                "taskType" to (task.payload::class.simpleName ?: "Unknown"),
                "runAttemptCount" to runAttemptCount,
                "totalRetries" to (runAttemptCount - 1)
            )
            return Json.encodeToString(log)
        }
    }

    data class TaskFailed(
        val taskId: TaskId,
        val task: TaskRequest,
        val error: Throwable?,
        val runAttemptCount: Int = 1,
    ) : TelemetryEvent() {
        override fun structured(): String {
            val log = mapOf(
                "event" to "TASK_FAILED",
                "timestamp" to Timestamp.now(),
                "taskId" to taskId.value,
                "taskType" to (task.payload::class.simpleName ?: "Unknown"),
                "runAttemptCount" to runAttemptCount,
                "errorType" to (error?.let { it::class.simpleName } ?: "Unknown"),
                "errorMessage" to error?.message
            )
            return Json.encodeToString(log)
        }
    }

    data class TaskCancelled(
        val taskId: TaskId,
        val task: TaskRequest,
        val runAttemptCount: Int = 1,
    ) : TelemetryEvent() {
        override fun structured(): String {
            val log = mapOf(
                "event" to "TASK_CANCELLED",
                "timestamp" to Timestamp.now(),
                "taskId" to taskId.value,
                "taskType" to (task.payload::class.simpleName ?: "Unknown"),
                "runAttemptCount" to runAttemptCount
            )
            return Json.encodeToString(log)
        }
    }

    data class TaskRetryScheduled(
        val taskId: TaskId,
        val task: TaskRequest,
        val attemptCount: Int,
        val nextRetryDelayMs: Long,
        val remainingRetries: Int,
        val errorCategory: RetryErrorCategory? = null,
        val errorMessage: String? = null,
        val backoffPolicy: BackoffPolicy? = null
    ) : TelemetryEvent() {
        override fun structured(): String {
            val log = mapOf(
                "event" to "TASK_RETRY_SCHEDULED",
                "timestamp" to Timestamp.now(),
                "taskId" to taskId.value,
                "taskType" to (task.payload::class.simpleName ?: "Unknown"),
                "attemptCount" to attemptCount,
                "remainingRetries" to remainingRetries,
                "nextRetryDelayMs" to nextRetryDelayMs,
                "errorCategory" to errorCategory?.name,
                "errorMessage" to errorMessage,
                "backoffPolicy" to backoffPolicy?.name,
                "priority" to task.priority.name,
                "requiresNetwork" to task.preconditions.requiresNetwork,
                "requiresCharging" to task.preconditions.requiresCharging
            )
            return Json.encodeToString(log)
        }
    }

    data class TaskRetryDecision(
        val taskId: TaskId,
        val decision: RetryDecision,
        val attemptCount: Int,
        val maxRetries: Int,
        val errorCategory: RetryErrorCategory,
        val reason: String
    ) : TelemetryEvent() {
        enum class RetryDecision {
            RETRY_SCHEDULED,
            MAX_RETRIES_EXCEEDED,
            PERMANENT_FAILURE,
            CIRCUIT_BREAKER_OPEN,
            RATE_LIMITED
        }

        override fun structured(): String {
            val log = mapOf(
                "event" to "TASK_RETRY_DECISION",
                "timestamp" to Timestamp.now(),
                "taskId" to taskId.value,
                "decision" to decision.name,
                "attemptCount" to attemptCount,
                "maxRetries" to maxRetries,
                "errorCategory" to errorCategory.name,
                "reason" to reason
            )
            return Json.encodeToString(log)
        }
    }

    data class TaskStatistics(
        val taskId: TaskId,
        val totalAttempts: Int,
        val successfulAttempts: Int,
        val failedAttempts: Int,
        val transientFailures: Int,
        val permanentFailures: Int,
        val averageRetryDelayMs: Long,
        val errorCategoryCounts: Map<String, Int>
    ) : TelemetryEvent() {
        override fun structured(): String {
            return Json.encodeToString(
                mapOf(
                    "event" to "TASK_STATISTICS",
                    "timestamp" to Timestamp.now(),
                    "taskId" to taskId.value,
                    "totalAttempts" to totalAttempts,
                    "successfulAttempts" to successfulAttempts,
                    "failedAttempts" to failedAttempts,
                    "transientFailures" to transientFailures,
                    "permanentFailures" to permanentFailures,
                    "averageRetryDelayMs" to averageRetryDelayMs,
                    "errorCategoryCounts" to errorCategoryCounts,
                    "successRate" to if (totalAttempts > 0) {
                        (successfulAttempts.toDouble() / totalAttempts * 100)
                    } else 0.0
                )
            )
        }
    }
}