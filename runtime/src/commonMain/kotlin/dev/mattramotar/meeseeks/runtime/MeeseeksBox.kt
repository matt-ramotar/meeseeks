package dev.mattramotar.meeseeks.runtime

import dev.mattramotar.meeseeks.runtime.impl.MeeseeksBoxSingleton
import kotlinx.coroutines.flow.Flow


/**
 * Central task manager.
 */
interface MeeseeksBox {


    /**
     * Summons a new [MrMeeseeks], scheduling the [task] for background execution.
     *
     * @param task The [Task] to schedule.
     * @return [MrMeeseeksId] identifying the summoned [MrMeeseeks].
     */
    fun summon(task: Task): MrMeeseeksId

    /**
     * Sends a specific [MrMeeseeks] back to the box, canceling its scheduled or ongoing work.
     *
     * @param id A [MrMeeseeksId] identifying a specific [MrMeeseeks].
     */
    fun sendBackToBox(id: MrMeeseeksId)

    /**
     * Sends all currently active [MrMeeseeks] back to the box, removing them from scheduling.
     */
    fun sendAllBackToBox()

    /**
     * Triggers an immediate scheduling check.
     */
    fun triggerCheckForDueTasks()

    /**
     * Returns the current status of the task with the given [id],
     * or `null` if the task does not exist.
     */
    fun getStatus(id: MrMeeseeksId): TaskStatus?

    /**
     * Returns a read-only list of all tasks known to Meeseeks, including
     * their ID, status, and the original [Task] definition.
     */
    fun getAllTasks(): List<ScheduledTask>

    /**
     * Convenience to “cancel & reschedule” a task in one call.
     * This will overwrite the existing task’s schedule/parameters
     * with [newTask], but keep the same [MrMeeseeksId].
     *
     * @return the same ID that was updated, for convenience
     * @throws IllegalStateException if no task with [id] exists
     */
    fun updateTask(id: MrMeeseeksId, newTask: Task): MrMeeseeksId

    /**
     * A real-time subscription to status changes via [Flow].
     */
    fun watchStatus(id: MrMeeseeksId): Flow<TaskStatus?>

    companion object {
        val value: MeeseeksBox
            get() {
                return MeeseeksBoxSingleton.meeseeksBox
            }
    }
}