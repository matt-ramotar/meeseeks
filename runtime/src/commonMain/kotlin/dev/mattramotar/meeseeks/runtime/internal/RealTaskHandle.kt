package dev.mattramotar.meeseeks.runtime.internal

import dev.mattramotar.meeseeks.runtime.BGTaskManager
import dev.mattramotar.meeseeks.runtime.TaskHandle
import dev.mattramotar.meeseeks.runtime.TaskId
import dev.mattramotar.meeseeks.runtime.TaskResult
import dev.mattramotar.meeseeks.runtime.TaskStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull

@PublishedApi
internal class RealTaskHandle(
    override val id: TaskId,
    private val bgTaskManager: BGTaskManager,
) : TaskHandle {

    override fun cancel() {
        bgTaskManager.cancel(id)
    }

    override fun observe(): Flow<TaskStatus> = bgTaskManager.observeStatus(id).filterNotNull()
}