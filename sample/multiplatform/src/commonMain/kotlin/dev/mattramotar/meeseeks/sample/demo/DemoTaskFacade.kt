package dev.mattramotar.meeseeks.sample.demo

import dev.mattramotar.meeseeks.runtime.BGTaskManager
import dev.mattramotar.meeseeks.runtime.ScheduledTask
import dev.mattramotar.meeseeks.runtime.TaskId
import dev.mattramotar.meeseeks.runtime.TaskRequest
import dev.mattramotar.meeseeks.runtime.TaskStatus
import dev.mattramotar.meeseeks.runtime.dsl.TaskRequestConfigurationScope
import dev.mattramotar.meeseeks.runtime.oneTime
import dev.mattramotar.meeseeks.runtime.periodic
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

public class DemoTaskFacade internal constructor(
    public val taskManager: BGTaskManager,
    private val telemetry: DemoTelemetry,
) {

    public val telemetryEvents: StateFlow<List<String>>
        get() = telemetry.events

    public fun schedule(request: DemoScheduleRequest): TaskId {
        return if (request.periodic) {
            schedulePeriodic(request)
        } else {
            scheduleOneTime(request)
        }
    }

    public fun scheduleOneTime(request: DemoScheduleRequest): TaskId {
        return when (request.scenario) {
            DemoScenario.SUCCESS -> taskManager.oneTime(
                payload = SuccessPayload,
                initialDelay = request.initialDelay,
            ) { applyRequest(request) }.id

            DemoScenario.RETRY_THEN_SUCCESS -> taskManager.oneTime(
                payload = RetryThenSuccessPayload,
                initialDelay = request.initialDelay,
            ) { applyRequest(request) }.id

            DemoScenario.PERMANENT_FAILURE -> taskManager.oneTime(
                payload = PermanentFailurePayload,
                initialDelay = request.initialDelay,
            ) { applyRequest(request) }.id

            DemoScenario.PERIODIC_HEARTBEAT -> taskManager.oneTime(
                payload = HeartbeatPayload,
                initialDelay = request.initialDelay,
            ) { applyRequest(request) }.id
        }
    }

    public fun schedulePeriodic(request: DemoScheduleRequest): TaskId {
        return when (request.scenario) {
            DemoScenario.SUCCESS -> taskManager.periodic(
                payload = SuccessPayload,
                every = request.interval,
                initialDelay = request.initialDelay,
            ) { applyRequest(request) }.id

            DemoScenario.RETRY_THEN_SUCCESS -> taskManager.periodic(
                payload = RetryThenSuccessPayload,
                every = request.interval,
                initialDelay = request.initialDelay,
            ) { applyRequest(request) }.id

            DemoScenario.PERMANENT_FAILURE -> taskManager.periodic(
                payload = PermanentFailurePayload,
                every = request.interval,
                initialDelay = request.initialDelay,
            ) { applyRequest(request) }.id

            DemoScenario.PERIODIC_HEARTBEAT -> taskManager.periodic(
                payload = HeartbeatPayload,
                every = request.interval,
                initialDelay = request.initialDelay,
            ) { applyRequest(request) }.id
        }
    }

    public fun reschedule(taskId: TaskId, request: DemoScheduleRequest): TaskId {
        val updatedRequest = if (request.periodic) {
            periodicRequest(request)
        } else {
            oneTimeRequest(request)
        }
        return taskManager.reschedule(taskId, updatedRequest)
    }

    public fun cancel(taskId: TaskId) {
        taskManager.cancel(taskId)
    }

    public fun cancelAll() {
        taskManager.cancelAll()
    }

    public fun reschedulePendingTasks() {
        taskManager.reschedulePendingTasks()
    }

    public fun getTaskStatus(taskId: TaskId): TaskStatus? {
        return taskManager.getTaskStatus(taskId)
    }

    public fun listTasks(): List<ScheduledTask> {
        return taskManager.listTasks()
    }

    public fun observeStatus(taskId: TaskId): Flow<TaskStatus?> {
        return taskManager.observeStatus(taskId)
    }

    public fun telemetrySnapshot(): DemoTelemetrySnapshot {
        return telemetry.snapshot()
    }

    public fun clearTelemetry() {
        telemetry.clear()
    }

    private fun oneTimeRequest(request: DemoScheduleRequest): TaskRequest {
        return when (request.scenario) {
            DemoScenario.SUCCESS -> TaskRequest.oneTime(SuccessPayload) { applyRequest(request) }
            DemoScenario.RETRY_THEN_SUCCESS -> TaskRequest.oneTime(RetryThenSuccessPayload) { applyRequest(request) }
            DemoScenario.PERMANENT_FAILURE -> TaskRequest.oneTime(PermanentFailurePayload) { applyRequest(request) }
            DemoScenario.PERIODIC_HEARTBEAT -> TaskRequest.oneTime(HeartbeatPayload) { applyRequest(request) }
        }
    }

    private fun periodicRequest(request: DemoScheduleRequest): TaskRequest {
        return when (request.scenario) {
            DemoScenario.SUCCESS -> TaskRequest.periodic(SuccessPayload, interval = request.interval) { applyRequest(request) }
            DemoScenario.RETRY_THEN_SUCCESS -> TaskRequest.periodic(RetryThenSuccessPayload, interval = request.interval) { applyRequest(request) }
            DemoScenario.PERMANENT_FAILURE -> TaskRequest.periodic(PermanentFailurePayload, interval = request.interval) { applyRequest(request) }
            DemoScenario.PERIODIC_HEARTBEAT -> TaskRequest.periodic(HeartbeatPayload, interval = request.interval) { applyRequest(request) }
        }
    }

    private fun <T : dev.mattramotar.meeseeks.runtime.TaskPayload> TaskRequestConfigurationScope<T>.applyRequest(
        request: DemoScheduleRequest,
    ) {
        if (request.requiresNetwork) {
            requireNetwork(true)
        }
        if (request.requiresCharging) {
            requireCharging(true)
        }
        if (request.requiresBatteryNotLow) {
            requireBatteryNotLow(true)
        }

        if (request.highPriority) {
            highPriority()
        } else {
            mediumPriority()
        }

        retryWithExponentialBackoff(
            initialDelay = request.backoffInitialDelay,
            maxAttempts = request.maxAttempts,
        )
    }
}
