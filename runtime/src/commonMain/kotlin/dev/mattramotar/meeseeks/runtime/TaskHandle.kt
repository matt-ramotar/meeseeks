package dev.mattramotar.meeseeks.runtime

import kotlinx.coroutines.flow.Flow

interface TaskHandle {
    val id: TaskId
    fun cancel()
    fun observe(): Flow<TaskStatus>

    // TODO: Support await
    // suspend fun await(): TaskResult
}