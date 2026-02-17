package dev.mattramotar.meeseeks.runtime.telemetry

import dev.mattramotar.meeseeks.runtime.TaskId
import dev.mattramotar.meeseeks.runtime.TaskRequest
import dev.mattramotar.meeseeks.runtime.internal.Timestamp
import dev.mattramotar.meeseeks.runtime.types.RetryErrorCategory
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

public sealed class TelemetryEvent {

    public abstract fun structured(): String

    public data class TaskScheduled(
        public val taskId: TaskId,
        public val task: TaskRequest
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

    public data class TaskStarted(
        public val taskId: TaskId,
        public val task: TaskRequest,
        public val runAttemptCount: Int = 1,
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

    public data class TaskSucceeded(
        public val taskId: TaskId,
        public val task: TaskRequest,
        public val runAttemptCount: Int = 1,
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

    public data class TaskFailed(
        public val taskId: TaskId,
        public val task: TaskRequest,
        public val error: Throwable?,
        public val runAttemptCount: Int = 1,
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

    public data class TaskCancelled(
        public val taskId: TaskId,
        public val task: TaskRequest,
        public val runAttemptCount: Int = 1,
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

    /**
     * Emitted when a platform task submission fails.
     */
    public data class TaskSubmitFailed(
        public val taskIdentifier: String,
        public val errorCode: Int,
        public val errorDescription: String?,
        public val isRetriable: Boolean
    ) : TelemetryEvent() {
        override fun structured(): String {
            val log = mapOf(
                "event" to "TASK_SUBMIT_FAILED",
                "timestamp" to Timestamp.now(),
                "taskIdentifier" to taskIdentifier,
                "errorCode" to errorCode,
                "errorDescription" to errorDescription,
                "isRetriable" to isRetriable,
                "errorType" to when (errorCode) {
                    1 -> "BGTaskSchedulerErrorUnavailable"
                    2 -> "BGTaskSchedulerErrorTooManyPendingTaskRequests"
                    3 -> "BGTaskSchedulerErrorNotPermitted"
                    else -> "Unknown"
                }
            )
            return Json.encodeToString(log)
        }
    }

    public data class TaskRetryScheduled(
        public val taskId: TaskId,
        public val task: TaskRequest,
        public val attemptCount: Int,
        public val nextRetryDelayMs: Long,
        public val remainingRetries: Int,
        public val errorCategory: RetryErrorCategory? = null,
        public val errorMessage: String? = null,
        public val backoffPolicy: String? = null
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
                "backoffPolicy" to backoffPolicy,
                "priority" to task.priority.name,
                "requiresNetwork" to task.preconditions.requiresNetwork,
                "requiresCharging" to task.preconditions.requiresCharging
            )
            return Json.encodeToString(log)
        }
    }

    public data class TaskRetryDecision(
        public val taskId: TaskId,
        public val decision: RetryDecision,
        public val attemptCount: Int,
        public val maxRetries: Int,
        public val errorCategory: RetryErrorCategory,
        public val reason: String
    ) : TelemetryEvent() {
        public enum class RetryDecision {
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

    public data class TaskStatistics(
        public val taskId: TaskId,
        public val totalAttempts: Int,
        public val successfulAttempts: Int,
        public val failedAttempts: Int,
        public val transientFailures: Int,
        public val permanentFailures: Int,
        public val averageRetryDelayMs: Long,
        public val errorCategoryCounts: Map<String, Int>
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

    /**
     * Emitted when the orphaned task watchdog recovers tasks that were left in enqueued state but not scheduled with the platform.
     */
    public data class OrphanedTasksRecovered(
        public val count: Int
    ) : TelemetryEvent() {
        override fun structured(): String {
            val log = mapOf(
                "event" to "ORPHANED_TASKS_RECOVERED",
                "timestamp" to Timestamp.now(),
                "count" to count
            )
            return Json.encodeToString(log)
        }
    }
}