package dev.mattramotar.meeseeks.runtime

import dev.mattramotar.meeseeks.runtime.impl.BackgroundTaskManagerSingleton
import kotlinx.coroutines.flow.Flow


/**
 * Central task manager.
 */
interface BackgroundTaskManager {


    /**
     * Summons a new [TaskWorker], scheduling the [task] for background execution.
     *
     * @param task The [Task] to schedule.
     * @return [TaskId] identifying the summoned [TaskWorker].
     */
    fun enqueue(task: Task): TaskId

    /**
     * Sends a specific [TaskWorker] back to the box, canceling its scheduled or ongoing work.
     *
     * @param id A [TaskId] identifying a specific [TaskWorker].
     */
    fun cancel(id: TaskId)

    /**
     * Sends all currently active [TaskWorker] back to the box, removing them from scheduling.
     */
    fun cancelAll()

    /**
     * Triggers an immediate scheduling check.
     */
    fun reschedulePendingTasks()

    /**
     * Returns the current status of the task with the given [id],
     * or `null` if the task does not exist.
     */
    fun getTaskStatus(id: TaskId): TaskStatus?

    /**
     * Returns a read-only list of all tasks known to Meeseeks, including
     * their ID, status, and the original [Task] definition.
     */
    fun listTasks(): List<ScheduledTask>

    /**
     * Convenience to “cancel & reschedule” a task in one call.
     * This will overwrite the existing task’s schedule/parameters
     * with [newTask], but keep the same [TaskId].
     *
     * @return the same ID that was updated, for convenience
     * @throws IllegalStateException if no task with [id] exists
     */
    fun reschedule(id: TaskId, newTask: Task): TaskId

    /**
     * A real-time subscription to status changes via [Flow].
     */
    fun watchStatus(id: TaskId): Flow<TaskStatus?>

    companion object Companion {
        val value: BackgroundTaskManager
            get() {
                return BackgroundTaskManagerSingleton.backgroundTaskManager
            }
    }
}