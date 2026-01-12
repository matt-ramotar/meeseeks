package dev.mattramotar.meeseeks.runtime.internal

import dev.mattramotar.meeseeks.runtime.BGTaskManagerConfig
import dev.mattramotar.meeseeks.runtime.TaskPayload
import dev.mattramotar.meeseeks.runtime.TaskRequest
import kotlin.test.Test
import kotlin.test.assertEquals

class WorkRequestFactoryAndroidTest {
    private object TestPayload : TaskPayload

    @Test
    fun createWorkRequest_storesOnlyTaskIdInInputData() {
        val taskId = "task-id-123"
        val request = TaskRequest.oneTime(TestPayload)
        val factory = WorkRequestFactory()

        val workRequest = factory.createWorkRequest(taskId, request, BGTaskManagerConfig())
        val inputData = workRequest.delegate.workSpec.input

        assertEquals(setOf(WorkRequestFactory.KEY_TASK_ID), inputData.keyValueMap.keys)
        assertEquals(taskId, inputData.getString(WorkRequestFactory.KEY_TASK_ID))
    }
}
