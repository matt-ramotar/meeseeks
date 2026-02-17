package dev.mattramotar.meeseeks.runtime

import dev.mattramotar.meeseeks.runtime.dsl.OneTimeTaskRequestConfigurationScope
import dev.mattramotar.meeseeks.runtime.dsl.PeriodicTaskRequestConfigurationScope
import dev.mattramotar.meeseeks.runtime.internal.RealTaskHandle
import kotlinx.coroutines.flow.Flow
import kotlin.time.Duration


/**
 * Central task manager.
 */
public interface BGTaskManager {


    /**
     * Schedules a new [Worker] for background execution.
     *
     * @param request The [TaskRequest] to schedule.
     * @return [TaskId] identifying the scheduled [Worker].
     * @throws IllegalArgumentException if [request] contains preconditions unsupported by this target.
     */
    public fun schedule(request: TaskRequest): TaskId

    /**
     * Cancels a specific [Worker], stopping scheduled or ongoing work.
     *
     * @param id A [TaskId] identifying a specific [Worker].
     */
    public fun cancel(id: TaskId)

    /**
     * Cancels all currently active [Worker] and removes them from scheduling.
     */
    public fun cancelAll()

    /**
     * Triggers an immediate scheduling check.
     */
    public fun reschedulePendingTasks()

    /**
     * Returns the current status of the task with the given [id],
     * or `null` if the task does not exist.
     */
    public fun getTaskStatus(id: TaskId): TaskStatus?

    /**
     * Returns a read-only list of all tasks known to Meeseeks, including
     * their ID, status, and original [TaskRequest].
     */
    public fun listTasks(): List<ScheduledTask>

    /**
     * Convenience to “cancel & reschedule” a task in one call.
     * This will overwrite the existing task’s schedule/parameters
     * with [updatedRequest], but keep the same [TaskId].
     *
     * @return the same ID that was updated, for convenience
     * @throws IllegalStateException if no task with [id] exists
     * @throws IllegalArgumentException if [updatedRequest] contains preconditions unsupported by this target.
     */
    public fun reschedule(id: TaskId, updatedRequest: TaskRequest): TaskId

    /**
     * A real-time subscription to status changes via [Flow].
     */
    public fun observeStatus(id: TaskId): Flow<TaskStatus?>
}

public inline fun <reified T : TaskPayload> BGTaskManager.oneTime(
    payload: T,
    initialDelay: Duration = Duration.ZERO,
    noinline configure: OneTimeTaskRequestConfigurationScope<T>.() -> Unit = {}
): TaskHandle {

    val configurationScope = OneTimeTaskRequestConfigurationScope(payload, initialDelay)
    configurationScope.configure()
    val request = configurationScope.build()
    return schedule(this, request)
}

public inline fun <reified T : TaskPayload> BGTaskManager.periodic(
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
