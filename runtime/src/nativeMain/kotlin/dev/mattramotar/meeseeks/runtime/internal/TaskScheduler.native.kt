package dev.mattramotar.meeseeks.runtime.internal

import dev.mattramotar.meeseeks.runtime.BGTaskRunner
import dev.mattramotar.meeseeks.runtime.TaskRequest
import dev.mattramotar.meeseeks.runtime.TaskResult
import dev.mattramotar.meeseeks.runtime.TaskSchedule
import dev.mattramotar.meeseeks.runtime.TaskStatus
import dev.mattramotar.meeseeks.runtime.db.MeeseeksDatabase
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.datetime.Clock
import platform.BackgroundTasks.BGAppRefreshTaskRequest
import platform.BackgroundTasks.BGProcessingTaskRequest
import platform.BackgroundTasks.BGTaskRequest
import platform.BackgroundTasks.BGTaskScheduler
import platform.Foundation.NSDate
import platform.Foundation.dateWithTimeIntervalSinceNow

@OptIn(ExperimentalForeignApi::class)
internal actual class TaskScheduler(
    private val database: MeeseeksDatabase,
    private val bgTaskScheduler: BGTaskScheduler
) {

    actual fun scheduleTask(
        taskId: Long,
        task: TaskRequest,
        workRequest: WorkRequest,
        existingWorkPolicy: ExistingWorkPolicy
    ) {
        val taskQueries = database.taskQueries
        val current = taskQueries.selectTaskByTaskId(taskId).executeAsOneOrNull() ?: return

        val existingSchedulerId = current.workRequestId
        val alreadyScheduled =
            existingSchedulerId != null && (current.status is TaskStatus.Pending || current.status is TaskStatus.Running)
        if (existingWorkPolicy == ExistingWorkPolicy.KEEP && alreadyScheduled) {
            return
        }
        if (existingWorkPolicy == ExistingWorkPolicy.REPLACE && existingSchedulerId != null && alreadyScheduled) {
            cancelWorkById(existingSchedulerId, task.schedule)
        }
        val bgTaskRequest = createBGTaskRequest(workRequest.id, task)
        bgTaskScheduler.submitTaskRequest(bgTaskRequest, null)
    }


    actual fun isScheduled(taskId: Long, taskSchedule: TaskSchedule): Boolean {
        val taskEntity = database.taskQueries
            .selectTaskByTaskId(taskId)
            .executeAsOneOrNull() ?: return false

        return (taskEntity.workRequestId != null) &&
            (taskEntity.status is TaskStatus.Pending || taskEntity.status is TaskStatus.Running)
    }

    actual fun cancelWorkById(schedulerId: String, taskSchedule: TaskSchedule) {
        val taskId = WorkRequestFactory.taskIdFromBGTaskIdentifier(schedulerId, taskSchedule)
        database.taskQueries.cancelTask(Clock.System.now().toEpochMilliseconds(), taskId)

        if (isIOS16OrLater()) {
            BGTaskScheduler.sharedScheduler.cancelTaskRequestWithIdentifier(schedulerId)
        }
    }

    actual fun cancelUniqueWork(uniqueWorkName: String, taskSchedule: TaskSchedule) {
        cancelWorkById(uniqueWorkName, taskSchedule)
    }

    actual fun cancelAllWorkByTag(tag: String) {
        val taskQueries = database.taskQueries
        val activeTasks = taskQueries.selectAllActive().executeAsList().filter { it.workRequestId != null }
        activeTasks.forEach { entity -> entity.workRequestId?.let { cancelWorkById(it, entity.schedule) } }
    }

    private fun createBGTaskRequest(identifier: String, request: TaskRequest): BGTaskRequest {
        val requiresNetwork = request.preconditions.requiresNetwork
        val requiresCharging = request.preconditions.requiresCharging

        val bgTaskRequest = if (requiresNetwork || requiresCharging) {
            BGProcessingTaskRequest(identifier).apply {
                requiresNetworkConnectivity = requiresNetwork
                requiresExternalPower = requiresCharging
            }
        } else {
            BGAppRefreshTaskRequest(identifier)
        }

        val earliestDelaySeconds = when (val schedule = request.schedule) {
            is TaskSchedule.OneTime -> {
                schedule.initialDelay.inWholeSeconds.toDouble()
            }

            is TaskSchedule.Periodic -> {
                schedule.interval.inWholeSeconds.toDouble()
            }
        }
        if (earliestDelaySeconds > 0) {
            bgTaskRequest.earliestBeginDate = NSDate.dateWithTimeIntervalSinceNow(earliestDelaySeconds)
        }

        return bgTaskRequest
    }
}
