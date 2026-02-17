package dev.mattramotar.meeseeks.runtime

import kotlinx.coroutines.flow.Flow

public interface TaskHandle {
    public val id: TaskId
    public fun cancel()
    public fun observe(): Flow<TaskStatus>
    /**
     * Suspends until this task reaches a terminal status.
     *
     * @return terminal status (`Completed`, `Failed`, or `Cancelled`)
     */
    public suspend fun await(): TaskStatus.Finished
}
