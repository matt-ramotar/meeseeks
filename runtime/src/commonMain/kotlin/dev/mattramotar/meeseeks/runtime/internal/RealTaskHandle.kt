package dev.mattramotar.meeseeks.runtime.internal

import dev.mattramotar.meeseeks.runtime.BGTaskManager
import dev.mattramotar.meeseeks.runtime.TaskHandle
import dev.mattramotar.meeseeks.runtime.TaskId
import dev.mattramotar.meeseeks.runtime.TaskStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.mapNotNull

@PublishedApi
internal class RealTaskHandle(
    override val id: TaskId,
    private val bgTaskManager: BGTaskManager,
) : TaskHandle {

    override fun cancel() {
        bgTaskManager.cancel(id)
    }

    override fun observe(): Flow<TaskStatus> = bgTaskManager.observeStatus(id).filterNotNull()

    override suspend fun await(): TaskStatus.Finished {
        return bgTaskManager.observeStatus(id)
            .mapNotNull { it as? TaskStatus.Finished }
            .first()
    }
}