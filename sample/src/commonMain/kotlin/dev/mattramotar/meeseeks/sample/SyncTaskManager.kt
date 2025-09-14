package dev.mattramotar.meeseeks.sample

import dev.mattramotar.meeseeks.runtime.BGTaskManager
import dev.mattramotar.meeseeks.runtime.TaskHandle
import dev.mattramotar.meeseeks.runtime.TaskId
import dev.mattramotar.meeseeks.runtime.TaskRequest
import dev.mattramotar.meeseeks.runtime.TaskStatus
import dev.mattramotar.meeseeks.runtime.dsl.TaskRequestConfigurationScope
import dev.mattramotar.meeseeks.runtime.oneTime
import dev.mattramotar.meeseeks.runtime.periodic
import dev.mattramotar.meeseeks.sample.shared.SyncPayload
import dev.mattramotar.meeseeks.sample.shared.Update
import kotlinx.coroutines.flow.Flow
import kotlin.time.Duration.Companion.minutes


class SyncTaskManager(
    private val bgTaskManager: BGTaskManager
) {

    private fun TaskRequestConfigurationScope<SyncPayload>.configureSync() {
        requireNetwork(true)
        requireCharging(false)
        requireBatteryNotLow(true)
        highPriority()
        retryWithExponentialBackoff(
            initialDelay = 1.minutes,
            maxAttempts = 3
        )
    }

    fun scheduleOneTimeTask(): TaskHandle =
        bgTaskManager.oneTime(SyncPayload) { configureSync() }

    fun schedulePeriodicTask(): TaskHandle =
        bgTaskManager.periodic(SyncPayload, every = 5.minutes) { configureSync() }

    fun observeStatus(taskId: TaskId): Flow<TaskStatus?> =
        bgTaskManager.observeStatus(taskId)

    fun reschedule(taskId: TaskId) {
        val updates = listOf(Update("new token"))
        val updatedRequest = TaskRequest.oneTime(SyncPayload)
        bgTaskManager.reschedule(taskId, updatedRequest)
    }
}


class FeatRepository {
    // TODO
}

class FeatViewModel {
    // TODO
}