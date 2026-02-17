package dev.mattramotar.meeseeks.runtime.internal

import dev.mattramotar.meeseeks.runtime.BGTaskManager
import dev.mattramotar.meeseeks.runtime.ScheduledTask
import dev.mattramotar.meeseeks.runtime.TaskId
import dev.mattramotar.meeseeks.runtime.TaskRequest
import dev.mattramotar.meeseeks.runtime.TaskStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class RealTaskHandleAwaitTest {

    @Test
    fun awaitReturnsCompletedWhenTaskFinishesSuccessfully() = runTest {
        val handle = RealTaskHandle(
            id = TaskId("task-1"),
            bgTaskManager = FakeBGTaskManager(
                statuses = flowOf(
                    TaskStatus.Pending,
                    TaskStatus.Running,
                    TaskStatus.Finished.Completed
                )
            )
        )

        val result = handle.await()

        assertEquals(TaskStatus.Finished.Completed, result)
    }

    @Test
    fun awaitReturnsFailedWhenTaskFinishesWithFailure() = runTest {
        val handle = RealTaskHandle(
            id = TaskId("task-2"),
            bgTaskManager = FakeBGTaskManager(
                statuses = flowOf(
                    null,
                    TaskStatus.Pending,
                    TaskStatus.Running,
                    TaskStatus.Finished.Failed
                )
            )
        )

        val result = handle.await()

        assertEquals(TaskStatus.Finished.Failed, result)
    }

    @Test
    fun awaitReturnsCancelledWhenTaskIsCancelled() = runTest {
        val handle = RealTaskHandle(
            id = TaskId("task-3"),
            bgTaskManager = FakeBGTaskManager(
                statuses = flowOf(
                    TaskStatus.Pending,
                    TaskStatus.Finished.Cancelled
                )
            )
        )

        val result = handle.await()

        assertEquals(TaskStatus.Finished.Cancelled, result)
    }
}

private class FakeBGTaskManager(
    private val statuses: Flow<TaskStatus?>
) : BGTaskManager {
    override fun schedule(request: TaskRequest): TaskId = error("Not required for this test")

    override fun cancel(id: TaskId) = Unit

    override fun cancelAll() = Unit

    override fun reschedulePendingTasks() = Unit

    override fun getTaskStatus(id: TaskId): TaskStatus? = null

    override fun listTasks(): List<ScheduledTask> = emptyList()

    override fun reschedule(id: TaskId, updatedRequest: TaskRequest): TaskId =
        error("Not required for this test")

    override fun observeStatus(id: TaskId): Flow<TaskStatus?> = statuses
}
