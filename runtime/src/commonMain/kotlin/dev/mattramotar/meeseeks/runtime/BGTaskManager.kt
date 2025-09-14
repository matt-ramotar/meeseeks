package dev.mattramotar.meeseeks.runtime

import dev.mattramotar.meeseeks.runtime.dsl.OneTimeTaskRequestConfigurationScope
import dev.mattramotar.meeseeks.runtime.dsl.PeriodicTaskRequestConfigurationScope
import dev.mattramotar.meeseeks.runtime.internal.RealTaskHandle
import kotlinx.coroutines.flow.Flow
import kotlin.time.Duration


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
     * with [updatedRequest], but keep the same [TaskId].
     *
     * @return the same ID that was updated, for convenience
     * @throws IllegalStateException if no task with [id] exists
     */
    fun reschedule(id: TaskId, updatedRequest: TaskRequest): TaskId

    /**
     * A real-time subscription to status changes via [Flow].
     */
    fun observeStatus(id: TaskId): Flow<TaskStatus?>
}

inline fun <reified T : TaskPayload> BGTaskManager.oneTime(
    payload: T,
    initialDelay: Duration = Duration.ZERO,
    noinline configure: OneTimeTaskRequestConfigurationScope<T>.() -> Unit = {}
): TaskHandle {

    val configurationScope = OneTimeTaskRequestConfigurationScope(payload, initialDelay)
    configurationScope.configure()
    val request = configurationScope.build()
    return schedule(this, request)
}

inline fun <reified T : TaskPayload> BGTaskManager.periodic(
    payload: T,
    every: Duration,
    initialDelay: Duration = Duration.ZERO,
    flexWindow: Duration = Duration.ZERO,
    noinline configure: PeriodicTaskRequestConfigurationScope<T>.() -> Unit = {}
): TaskHandle {
    val configurationScope = PeriodicTaskRequestConfigurationScope(payload, initialDelay, every, flexWindow)
    configurationScope.configure()
    val request = configurationScope.build()
    return schedule(this, request)
}

@PublishedApi
internal fun schedule(bgTaskManager: BGTaskManager, request: TaskRequest): TaskHandle {
    val id = bgTaskManager.schedule(request)
    return RealTaskHandle(id, bgTaskManager)
}