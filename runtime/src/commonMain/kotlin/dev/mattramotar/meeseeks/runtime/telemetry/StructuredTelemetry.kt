package dev.mattramotar.meeseeks.runtime.telemetry

import dev.mattramotar.meeseeks.runtime.TaskId
import dev.mattramotar.meeseeks.runtime.internal.Timestamp
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json


/**
 * Structured telemetry implementation that provides comprehensive logging and statistics.
 */
class StructuredTelemetry(
    private val config: Config = Config()
) : Telemetry {

    /**
     * Configuration for structured telemetry.
     * @param enableStatistics Enable task statistics tracking.
     * @param enableStructuredLogs Enable storing structured logs.
     * @param maxStatsRetention Max tasks to keep stats for.
     * @param logger Logger implementation for output.
     */
    data class Config(
        val enableStatistics: Boolean = true,
        val enableStructuredLogs: Boolean = true,
        val maxStatsRetention: Int = 1000,
        val logger: Logger = ConsoleLogger()
    )

    private val json = Json {
        prettyPrint = false
        ignoreUnknownKeys = true
    }

    /**
     * Task statistics tracking.
     */
    @Serializable
    data class TaskStats(
        val taskId: Long,
        var totalAttempts: Int = 0,
        var successfulAttempts: Int = 0,
        var failedAttempts: Int = 0,
        var transientFailures: Int = 0,
        var permanentFailures: Int = 0,
        val errorCategoryCounts: MutableMap<String, Int> = mutableMapOf(),
        val retryDelays: MutableList<Long> = mutableListOf(),
        var lastErrorCategory: String? = null,
        var lastErrorMessage: String? = null,
        var firstAttemptTime: Long? = null,
        var lastAttemptTime: Long? = null,
        var totalExecutionTimeMs: Long = 0
    ) {
        fun toStatisticsEvent(taskId: TaskId): TelemetryEvent.TaskStatistics {
            val avgDelay = if (retryDelays.isNotEmpty()) {
                retryDelays.sum() / retryDelays.size
            } else 0L

            return TelemetryEvent.TaskStatistics(
                taskId = taskId,
                totalAttempts = totalAttempts,
                successfulAttempts = successfulAttempts,
                failedAttempts = failedAttempts,
                transientFailures = transientFailures,
                permanentFailures = permanentFailures,
                averageRetryDelayMs = avgDelay,
                errorCategoryCounts = errorCategoryCounts.toMap()
            )
        }
    }

    private val taskStats = mutableMapOf<Long, TaskStats>()
    private val eventLog = mutableListOf<String>()

    override suspend fun onEvent(event: TelemetryEvent) {
        val structuredLog = event.structured()
        logEvent(event, structuredLog)

        if (config.enableStructuredLogs) {
            eventLog.add(structuredLog)
        }

        if (config.enableStatistics) {
            updateStatistics(event)
        }

        when (event) {
            is TelemetryEvent.TaskRetryScheduled -> {
                handleRetryScheduled(event)
            }

            is TelemetryEvent.TaskRetryDecision -> {
                handleRetryDecision(event)
            }

            is TelemetryEvent.TaskSucceeded -> {
                handleTaskSuccess(event)
            }

            is TelemetryEvent.TaskFailed -> {
                handleTaskFailure(event)
            }

            is TelemetryEvent.TaskStarted -> {
                handleTaskStarted(event)
            }

            else -> {
                // Other events are handled by default processing
            }
        }

        if (taskStats.size > config.maxStatsRetention) {
            cleanupOldStats()
        }
    }

    /**
     * Get statistics for a specific task.
     */
    fun getTaskStatistics(taskId: TaskId): TelemetryEvent.TaskStatistics? {
        return taskStats[taskId.value]?.toStatisticsEvent(taskId)
    }

    /**
     * Get aggregated statistics across all tasks.
     */
    fun getAggregatedStatistics(): Map<String, Any?> {
        if (taskStats.isEmpty()) {
            return emptyMap()
        }

        val totalTasks = taskStats.size
        val totalAttempts = taskStats.values.sumOf { it.totalAttempts }
        val totalSuccesses = taskStats.values.sumOf { it.successfulAttempts }
        val totalFailures = taskStats.values.sumOf { it.failedAttempts }
        val totalTransientFailures = taskStats.values.sumOf { it.transientFailures }
        val totalPermanentFailures = taskStats.values.sumOf { it.permanentFailures }

        val errorCategoryTotals = mutableMapOf<String, Int>()

        taskStats.values.forEach { stats ->
            stats.errorCategoryCounts.forEach { (category) ->
                errorCategoryTotals[category] = errorCategoryTotals.getOrElse(category) { 0 } + 1
            }
        }

        val avgRetryDelayMs = taskStats.values
            .flatMap { it.retryDelays }
            .takeIf { it.isNotEmpty() }
            ?.average()
            ?.toLong() ?: 0L

        return mapOf(
            "totalTasks" to totalTasks,
            "totalAttempts" to totalAttempts,
            "totalSuccesses" to totalSuccesses,
            "totalFailures" to totalFailures,
            "totalTransientFailures" to totalTransientFailures,
            "totalPermanentFailures" to totalPermanentFailures,
            "successRate" to if (totalAttempts > 0) {
                (totalSuccesses.toDouble() / totalAttempts * 100)
            } else 0.0,
            "averageRetryDelayMs" to avgRetryDelayMs,
            "errorCategoryCounts" to errorCategoryTotals,
            "mostCommonErrorCategory" to errorCategoryTotals.maxByOrNull { it.value }?.key
        )
    }

    /**
     * Export all events as JSON.
     */
    fun exportEventsAsJson(): String {
        return json.encodeToString(eventLog)
    }

    /**
     * Export statistics as JSON.
     */
    fun exportStatisticsAsJson(): String {
        val stats = taskStats.mapValues { (_, stats) ->
            mapOf(
                "totalAttempts" to stats.totalAttempts,
                "successfulAttempts" to stats.successfulAttempts,
                "failedAttempts" to stats.failedAttempts,
                "transientFailures" to stats.transientFailures,
                "permanentFailures" to stats.permanentFailures,
                "errorCategoryCounts" to stats.errorCategoryCounts,
                "averageRetryDelay" to if (stats.retryDelays.isNotEmpty()) {
                    stats.retryDelays.average()
                } else 0.0,
                "lastError" to mapOf(
                    "category" to stats.lastErrorCategory,
                    "message" to stats.lastErrorMessage
                )
            )
        }
        return json.encodeToString(stats)
    }

    /**
     * Clear statistics for a specific task.
     */
    fun clearTaskStatistics(taskId: TaskId) {
        taskStats.remove(taskId.value)
    }

    /**
     * Clear all statistics and logs.
     */
    fun clearAll() {
        taskStats.clear()
        eventLog.clear()
    }

    private fun updateStatistics(event: TelemetryEvent) {
        when (event) {
            is TelemetryEvent.TaskStarted -> {
                val stats = taskStats.getOrPut(event.taskId.value) { TaskStats(event.taskId.value) }
                stats.totalAttempts = maxOf(stats.totalAttempts, event.runAttemptCount)
                if (stats.firstAttemptTime == null) {
                    stats.firstAttemptTime = Timestamp.now()
                }
                stats.lastAttemptTime = Timestamp.now()
            }

            is TelemetryEvent.TaskSucceeded -> {
                val stats = taskStats.getOrPut(event.taskId.value) { TaskStats(event.taskId.value) }
                stats.successfulAttempts++
                stats.lastAttemptTime?.let { lastTime ->
                    stats.totalExecutionTimeMs += (Timestamp.now() - lastTime)
                }
            }

            is TelemetryEvent.TaskFailed -> {
                val stats = taskStats.getOrPut(event.taskId.value) { TaskStats(event.taskId.value) }
                stats.failedAttempts++

                val isTransient = event.error?.let { error ->
                    error::class.simpleName?.contains("Transient") == true ||
                        error.message?.contains("retry", ignoreCase = true) == true
                } ?: false

                if (isTransient) {
                    stats.transientFailures++
                } else {
                    stats.permanentFailures++
                }

                stats.lastErrorMessage = event.error?.message
            }

            is TelemetryEvent.TaskRetryScheduled -> {
                val stats = taskStats.getOrPut(event.taskId.value) { TaskStats(event.taskId.value) }
                stats.retryDelays.add(event.nextRetryDelayMs)
                event.errorCategory?.let { category ->
                    stats.errorCategoryCounts[category.name] =
                        stats.errorCategoryCounts.getOrElse(category.name) { 0 } + 1
                    stats.lastErrorCategory = category.name
                }
                event.errorMessage?.let {
                    stats.lastErrorMessage = it
                }
            }

            else -> {
                // Other events don't affect statistics
            }
        }
    }

    private fun handleRetryScheduled(event: TelemetryEvent.TaskRetryScheduled) {
        if (config.logger.isEnabled(LogLevel.INFO)) {
            val message = buildString {
                append("Task ${event.taskId.value} retry scheduled: ")
                append("attempt ${event.attemptCount}, ")
                append("${event.remainingRetries} retries remaining, ")
                append("delay ${event.nextRetryDelayMs}ms")
                event.errorCategory?.let { append(", category: ${it.name}") }
            }
            config.logger.log(LogLevel.INFO, message)
        }
    }

    private fun handleRetryDecision(event: TelemetryEvent.TaskRetryDecision) {
        if (config.logger.isEnabled(LogLevel.INFO)) {
            config.logger.log(
                LogLevel.INFO,
                "Task ${event.taskId.value}: ${event.decision} - ${event.reason}"
            )
        }
    }

    private fun handleTaskSuccess(event: TelemetryEvent.TaskSucceeded) {
        if (config.logger.isEnabled(LogLevel.INFO)) {
            val stats = taskStats[event.taskId.value]
            val retryInfo = if (event.runAttemptCount > 1) {
                " after ${event.runAttemptCount} attempts"
            } else ""
            config.logger.log(LogLevel.INFO, "Task ${event.taskId.value} completed$retryInfo")

            stats?.let {
                if (it.totalAttempts > 1) {
                    val statsMessage = "Task ${event.taskId.value}: " +
                        "total attempts=${it.totalAttempts}, " +
                        "failures=${it.failedAttempts}, " +
                        "avg retry delay=${it.retryDelays.average().toLong()}ms"
                    config.logger.log(LogLevel.INFO, statsMessage)
                }
            }
        }
    }

    private fun handleTaskFailure(event: TelemetryEvent.TaskFailed) {
        val level = if (event.runAttemptCount >= 3) LogLevel.ERROR else LogLevel.WARN
        if (config.logger.isEnabled(level)) {
            val message = "Task ${event.taskId.value} failed (attempt ${event.runAttemptCount}): ${event.error?.message}"
            if (level == LogLevel.ERROR) {
                config.logger.error(message, event.error)
            } else {
                config.logger.log(level, message)
            }
        }
    }

    private fun handleTaskStarted(event: TelemetryEvent.TaskStarted) {
        if (config.logger.isEnabled(LogLevel.DEBUG)) {
            config.logger.log(
                LogLevel.DEBUG,
                "Task ${event.taskId.value} started (attempt ${event.runAttemptCount})"
            )
        }
    }

    private fun logEvent(event: TelemetryEvent, structuredLog: String) {
        val level = determineLogLevel(event)
        if (config.logger.isEnabled(level)) {
            config.logger.logStructured(level, "TELEMETRY", structuredLog)
        }
    }

    private fun determineLogLevel(event: TelemetryEvent): LogLevel {
        return when (event) {
            is TelemetryEvent.TaskFailed -> {
                if (event.runAttemptCount >= 3) LogLevel.ERROR else LogLevel.WARN
            }

            is TelemetryEvent.TaskRetryScheduled,
            is TelemetryEvent.TaskRetryDecision -> LogLevel.INFO

            is TelemetryEvent.TaskSucceeded -> LogLevel.INFO
            is TelemetryEvent.TaskStarted,
            is TelemetryEvent.TaskScheduled -> LogLevel.DEBUG

            is TelemetryEvent.TaskCancelled -> LogLevel.WARN
            is TelemetryEvent.TaskStatistics -> LogLevel.DEBUG
            is TelemetryEvent.TaskSubmitFailed -> {
                if (event.isRetriable) LogLevel.WARN else LogLevel.ERROR
            }
        }
    }

    private fun cleanupOldStats() {
        val toRemove = taskStats.size - config.maxStatsRetention
        if (toRemove > 0) {
            taskStats.entries
                .sortedBy { it.value.firstAttemptTime ?: 0L }
                .take(toRemove)
                .forEach { taskStats.remove(it.key) }
        }
    }
}