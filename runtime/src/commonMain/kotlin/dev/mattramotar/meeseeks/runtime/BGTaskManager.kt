package dev.mattramotar.meeseeks.runtime

import kotlinx.coroutines.flow.Flow


/**
 * Central task manager.
 */
interface BGTaskManager {


    /**
     * Summons a new [Worker], scheduling the [request] for background execution.
     *
     * @param request The [TaskRequest] to schedule.
     * @return [TaskId] identifying the summoned [Worker].
     */
    fun schedule(request: TaskRequest): TaskId

    /**
     * Sends a specific [Worker] back to the box, canceling its scheduled or ongoing work.
     *
     * @param id A [TaskId] identifying a specific [Worker].
     */
    fun cancel(id: TaskId)

    /**
     * Sends all currently active [Worker] back to the box, removing them from scheduling.
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
     * their ID, status, and the original [LegacyTask] task.
     */
    fun listTasks(): List<ScheduledTask>

    /**
     * Convenience to “cancel & reschedule” a task in one call.
     * This will overwrite the existing task’s schedule/parameters
     * with [request], but keep the same [TaskId].
     *
     * @return the same ID that was updated, for convenience
     * @throws IllegalStateException if no task with [id] exists
     */
    fun reschedule(id: TaskId, request: TaskRequest): TaskId

    /**
     * A real-time subscription to status changes via [Flow].
     */
    fun observeStatus(id: TaskId): Flow<TaskStatus?>

    companion object {
        fun builder(appContext: AppContext): BGTaskManagerBuilder {
            return BGTaskManagerBuilder(appContext)
        }
    }
}