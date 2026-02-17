package dev.mattramotar.meeseeks.runtime

import kotlinx.coroutines.flow.Flow

interface TaskHandle {
    val id: TaskId
    fun cancel()
    fun observe(): Flow<TaskStatus>
    /**
     * Suspends until this task reaches a terminal status.
     *
     * @return terminal status (`Completed`, `Failed`, or `Cancelled`)
     */
    suspend fun await(): TaskStatus.Finished
}