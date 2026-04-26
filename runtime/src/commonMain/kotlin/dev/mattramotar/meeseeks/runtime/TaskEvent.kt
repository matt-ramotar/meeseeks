package dev.mattramotar.meeseeks.runtime

import kotlinx.coroutines.flow.Flow

/**
 * Durable task outcome that can be replayed after the caller was not observing
 * the task live.
 *
 * [id] is a monotonically increasing event cursor from Meeseeks persistence.
 * Store the highest replayed [id] and pass it to [BGTaskManager.replayTerminalEvents]
 * on the next app start to recover only newer terminal outcomes.
 *
 * Terminal replay only reports outcomes that end the current task run:
 * - one-time task success
 * - permanent task failure, including retry exhaustion
 * - explicit cancellation
 *
 * Periodic task success schedules the next activation and is not terminal.
 * Periodic tasks emit replayable terminal events only when they fail permanently
 * or are cancelled.
 *
 * Retention follows Meeseeks task persistence. Events are retained while their
 * task and log rows are retained; Meeseeks does not currently prune task logs
 * automatically.
 */
public data class TaskEvent(
    public val id: Long,
    public val taskId: TaskId,
    public val outcome: TaskEventOutcome,
    public val createdAt: Long,
    public val attempt: Int,
    public val message: String?,
)

/**
 * Public terminal outcomes exposed by durable task event replay.
 */
public enum class TaskEventOutcome {
    Success,
    Failure,
    Cancelled,
}

/**
 * Capability implemented by Meeseeks task managers that can replay durable
 * terminal task events.
 */
public interface TaskEventReplay {
    /**
     * Returns replayable terminal events already persisted for [taskId].
     */
    public fun getTaskEvents(taskId: TaskId): List<TaskEvent>

    /**
     * Observes the persisted replayable terminal events for [taskId].
     *
     * This is durable-log observation, not a replacement for
     * [TaskHandle.observe] or [BGTaskManager.observeStatus]. Live status
     * observation can miss app-death transitions; this API re-reads persisted
     * terminal outcomes.
     */
    public fun observeTaskEvents(taskId: TaskId): Flow<List<TaskEvent>>

    /**
     * Returns all replayable terminal events with event ids greater than
     * [sinceEventId].
     */
    public fun replayTerminalEvents(sinceEventId: Long = 0L): List<TaskEvent>
}

/**
 * Returns replayable terminal events already persisted for [taskId].
 */
public fun BGTaskManager.getTaskEvents(taskId: TaskId): List<TaskEvent> {
    return eventReplay().getTaskEvents(taskId)
}

/**
 * Observes persisted terminal event replay for [taskId].
 */
public fun BGTaskManager.observeTaskEvents(taskId: TaskId): Flow<List<TaskEvent>> {
    return eventReplay().observeTaskEvents(taskId)
}

/**
 * Replays all terminal task events with event ids greater than [sinceEventId].
 */
public fun BGTaskManager.replayTerminalEvents(sinceEventId: Long = 0L): List<TaskEvent> {
    return eventReplay().replayTerminalEvents(sinceEventId)
}

private fun BGTaskManager.eventReplay(): TaskEventReplay {
    return this as? TaskEventReplay
        ?: throw UnsupportedOperationException("This BGTaskManager does not support durable task event replay.")
}
