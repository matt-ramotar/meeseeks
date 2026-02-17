package dev.mattramotar.meeseeks.runtime.internal

import dev.mattramotar.meeseeks.runtime.BGTaskManagerConfig
import dev.mattramotar.meeseeks.runtime.TaskPayload
import dev.mattramotar.meeseeks.runtime.TaskPreconditions
import dev.mattramotar.meeseeks.runtime.TaskRequest
import dev.mattramotar.meeseeks.runtime.TaskSchedule
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class TaskSchedulerJsIntegrationTest {

    private object TestPayload : TaskPayload

    private val workRequestFactory = WorkRequestFactory()
    private val config = BGTaskManagerConfig()

    @Test
    fun scheduleAndCancelUniqueWorkUpdatesScheduledState() {
        val scheduler = TaskScheduler()
        val taskId = "js-unique-cancel-task"
        val request = oneTimeRequest(initialDelayMs = 90.seconds.inWholeMilliseconds)
        val workRequest = workRequestFactory.createWorkRequest(taskId, request, config)

        scheduler.scheduleTask(taskId, request, workRequest, ExistingWorkPolicy.KEEP)
        assertTrue(scheduler.isScheduled(taskId, request.schedule))

        scheduler.cancelUniqueWork(
            uniqueWorkName = WorkRequestFactory.uniqueWorkNameFor(taskId, request.schedule),
            taskSchedule = request.schedule
        )

        assertFalse(scheduler.isScheduled(taskId, request.schedule))
    }

    @Test
    fun cancelWorkByIdUsingSchedulerIdentifierUpdatesScheduledState() {
        val scheduler = TaskScheduler()
        val taskId = "js-cancel-by-id-task"
        val request = periodicRequest(initialDelayMs = 120.seconds.inWholeMilliseconds)
        val workRequest = workRequestFactory.createWorkRequest(taskId, request, config)

        scheduler.scheduleTask(taskId, request, workRequest, ExistingWorkPolicy.KEEP)
        assertTrue(scheduler.isScheduled(taskId, request.schedule))

        // JS scheduler tracks unique work names internally as scheduler identifiers.
        val schedulerId = WorkRequestFactory.uniqueWorkNameFor(taskId, request.schedule)
        scheduler.cancelWorkById(schedulerId, request.schedule)

        assertFalse(scheduler.isScheduled(taskId, request.schedule))
    }

    @Test
    fun cancelAllWorkByTagCancelsBothOneTimeAndPeriodicTasks() {
        val scheduler = TaskScheduler()
        val oneTimeTaskId = "js-cancel-all-one-time"
        val periodicTaskId = "js-cancel-all-periodic"

        val oneTimeRequest = oneTimeRequest(initialDelayMs = 60.seconds.inWholeMilliseconds)
        val periodicRequest = periodicRequest(initialDelayMs = 180.seconds.inWholeMilliseconds)

        scheduler.scheduleTask(
            oneTimeTaskId,
            oneTimeRequest,
            workRequestFactory.createWorkRequest(oneTimeTaskId, oneTimeRequest, config),
            ExistingWorkPolicy.KEEP
        )
        scheduler.scheduleTask(
            periodicTaskId,
            periodicRequest,
            workRequestFactory.createWorkRequest(periodicTaskId, periodicRequest, config),
            ExistingWorkPolicy.KEEP
        )

        assertTrue(scheduler.isScheduled(oneTimeTaskId, oneTimeRequest.schedule))
        assertTrue(scheduler.isScheduled(periodicTaskId, periodicRequest.schedule))

        scheduler.cancelAllWorkByTag(WorkRequestFactory.WORK_REQUEST_TAG)

        assertFalse(scheduler.isScheduled(oneTimeTaskId, oneTimeRequest.schedule))
        assertFalse(scheduler.isScheduled(periodicTaskId, periodicRequest.schedule))
    }

    @Test
    fun immediateNetworkTaskUsesNetworkPathAndStillTracksSchedulingState() {
        val scheduler = TaskScheduler()
        val taskId = "js-immediate-network-task"
        val request = oneTimeRequest(initialDelayMs = 0L, requiresNetwork = true)
        val workRequest = workRequestFactory.createWorkRequest(taskId, request, config)

        scheduler.scheduleTask(taskId, request, workRequest, ExistingWorkPolicy.KEEP)

        assertTrue(scheduler.isScheduled(taskId, request.schedule))

        scheduler.cancelUniqueWork(
            uniqueWorkName = WorkRequestFactory.uniqueWorkNameFor(taskId, request.schedule),
            taskSchedule = request.schedule
        )
        assertFalse(scheduler.isScheduled(taskId, request.schedule))
    }

    private fun oneTimeRequest(initialDelayMs: Long, requiresNetwork: Boolean = false): TaskRequest {
        return TaskRequest(
            payload = TestPayload,
            preconditions = TaskPreconditions(requiresNetwork = requiresNetwork),
            schedule = TaskSchedule.OneTime(initialDelayMs.milliseconds)
        )
    }

    private fun periodicRequest(initialDelayMs: Long): TaskRequest {
        return TaskRequest(
            payload = TestPayload,
            schedule = TaskSchedule.Periodic(
                initialDelay = initialDelayMs.milliseconds,
                interval = 5.seconds,
                flexWindow = 1.seconds
            )
        )
    }
}
