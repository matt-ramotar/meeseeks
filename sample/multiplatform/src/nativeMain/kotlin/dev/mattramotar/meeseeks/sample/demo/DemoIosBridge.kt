package dev.mattramotar.meeseeks.sample.demo

import dev.mattramotar.meeseeks.runtime.AppContext
import dev.mattramotar.meeseeks.runtime.TaskId

public object DemoIosAppContext : AppContext()

/**
 * Swift-friendly bridge wrapper for the iOS sample shell.
 */
public class DemoIosBridge(
    encryptionEnabled: Boolean = false,
) {

    private val facade = DemoTaskFacadeFactory.create(
        appContext = DemoIosAppContext,
        config = DemoSampleConfig(encryptionEnabled = encryptionEnabled),
    )

    public fun availableScenarios(): List<String> {
        return DemoScenario.entries.map { it.name }
    }

    public fun schedule(
        scenarioName: String,
        periodic: Boolean,
        requiresNetwork: Boolean = false,
        requiresCharging: Boolean = false,
        requiresBatteryNotLow: Boolean = false,
        highPriority: Boolean = false,
    ): String {
        return runCatching {
            val taskId = facade.schedule(
                request = buildRequest(
                    scenarioName = scenarioName,
                    periodic = periodic,
                    requiresNetwork = requiresNetwork,
                    requiresCharging = requiresCharging,
                    requiresBatteryNotLow = requiresBatteryNotLow,
                    highPriority = highPriority,
                )
            )
            taskId.value
        }.getOrElse { error ->
            "ERROR: ${error.message ?: "Unknown schedule error"}"
        }
    }

    public fun scheduleOneTime(scenarioName: String): String {
        return schedule(scenarioName = scenarioName, periodic = false)
    }

    public fun schedulePeriodic(scenarioName: String): String {
        return schedule(scenarioName = scenarioName, periodic = true)
    }

    public fun reschedule(
        taskId: String,
        scenarioName: String,
        periodic: Boolean,
        requiresNetwork: Boolean = false,
        requiresCharging: Boolean = false,
        requiresBatteryNotLow: Boolean = false,
        highPriority: Boolean = false,
    ): String {
        return runCatching {
            val updatedId = facade.reschedule(
                taskId = TaskId(taskId),
                request = buildRequest(
                    scenarioName = scenarioName,
                    periodic = periodic,
                    requiresNetwork = requiresNetwork,
                    requiresCharging = requiresCharging,
                    requiresBatteryNotLow = requiresBatteryNotLow,
                    highPriority = highPriority,
                )
            )
            updatedId.value
        }.getOrElse { error ->
            "ERROR: ${error.message ?: "Unknown reschedule error"}"
        }
    }

    public fun cancel(taskId: String) {
        facade.cancel(TaskId(taskId))
    }

    public fun cancelAll() {
        facade.cancelAll()
    }

    public fun reschedulePendingTasks() {
        facade.reschedulePendingTasks()
    }

    public fun listTaskLines(): List<String> {
        return facade.listTasks().map { task ->
            "${task.id.value} | ${task.status} | ${task.task.payload::class.simpleName ?: "payload"}"
        }
    }

    public fun telemetryLines(limit: Int): List<String> {
        val safeLimit = if (limit <= 0) 1 else limit
        return facade.telemetryEvents.value.takeLast(safeLimit)
    }

    public fun telemetryStatisticsJson(): String {
        return facade.telemetrySnapshot().statisticsJson
    }

    public fun telemetryEventsJson(): String {
        return facade.telemetrySnapshot().eventsJson
    }

    public fun clearTelemetry() {
        facade.clearTelemetry()
    }

    private fun buildRequest(
        scenarioName: String,
        periodic: Boolean,
        requiresNetwork: Boolean,
        requiresCharging: Boolean,
        requiresBatteryNotLow: Boolean,
        highPriority: Boolean,
    ): DemoScheduleRequest {
        return DemoScheduleRequest(
            scenario = DemoScenario.fromName(scenarioName),
            periodic = periodic,
            requiresNetwork = requiresNetwork,
            requiresCharging = requiresCharging,
            requiresBatteryNotLow = requiresBatteryNotLow,
            highPriority = highPriority,
        )
    }
}
