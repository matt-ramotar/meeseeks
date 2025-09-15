package dev.mattramotar.meeseeks.runtime.internal

import dev.mattramotar.meeseeks.runtime.TaskRequest
import dev.mattramotar.meeseeks.runtime.TaskSchedule
import dev.mattramotar.meeseeks.runtime.TaskStatus
import dev.mattramotar.meeseeks.runtime.db.MeeseeksDatabase
import dev.mattramotar.meeseeks.runtime.internal.db.model.toPublicStatus
import kotlinx.cinterop.ExperimentalForeignApi

/**
 * Adapter that bridges the commonMain [TaskScheduler] interface with the native-specific [NativeTaskScheduler].
 */
@OptIn(ExperimentalForeignApi::class)
internal actual class TaskScheduler(
    private val database: MeeseeksDatabase,
    private val nativeScheduler: NativeTaskScheduler
) {

    actual fun scheduleTask(
        taskId: Long,
        task: TaskRequest,
        workRequest: WorkRequest,
        existingWorkPolicy: ExistingWorkPolicy
    ) {
        nativeScheduler.schedule(task.preconditions, task.schedule)
    }

    /**
     * On iOS, "scheduled" means the task is PENDING/RUNNING in the database, as the OS handles the actual wakeup timing
     */
    actual fun isScheduled(taskId: Long, taskSchedule: TaskSchedule): Boolean {
        val taskSpec = database.taskSpecQueries
            .selectTaskById(taskId)
            .executeAsOneOrNull() ?: return false

        val status = taskSpec.state.toPublicStatus()
        return status is TaskStatus.Pending || status is TaskStatus.Running
    }

    // Cancellation is handled by DB updates in common code
    actual fun cancelWorkById(schedulerId: String, taskSchedule: TaskSchedule) {
        // No-op. We do not cancel specific platform requests as they are shared
    }

    actual fun cancelUniqueWork(uniqueWorkName: String, taskSchedule: TaskSchedule) {
        // No-op
    }

    /**
     * Cancels all pending requests with the OS
     */
    actual fun cancelAllWorkByTag(tag: String) {
        nativeScheduler.cancelAllPlatformRequests()
    }
}