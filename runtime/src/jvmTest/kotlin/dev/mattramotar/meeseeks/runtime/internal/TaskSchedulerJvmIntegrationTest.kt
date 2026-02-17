package dev.mattramotar.meeseeks.runtime.internal

import dev.mattramotar.meeseeks.runtime.BGTaskManagerConfig
import dev.mattramotar.meeseeks.runtime.TaskPayload
import dev.mattramotar.meeseeks.runtime.TaskRequest
import dev.mattramotar.meeseeks.runtime.TaskSchedule
import org.quartz.Scheduler
import org.quartz.impl.StdSchedulerFactory
import java.util.Properties
import java.util.UUID
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds

class TaskSchedulerJvmIntegrationTest {

    private object TestPayload : TaskPayload

    private lateinit var scheduler: Scheduler
    private lateinit var taskScheduler: TaskScheduler
    private lateinit var workRequestFactory: WorkRequestFactory

    @BeforeTest
    fun setUp() {
        scheduler = createInMemoryScheduler()
        taskScheduler = TaskScheduler(scheduler)
        workRequestFactory = WorkRequestFactory()
    }

    @AfterTest
    fun tearDown() {
        if (::scheduler.isInitialized) {
            scheduler.shutdown(true)
        }
    }

    @Test
    fun keepPolicyDoesNotReplaceExistingSchedule() {
        val taskId = "keep-policy-task"
        val firstRequest = oneTimeRequest(24.hours)
        val secondRequest = oneTimeRequest(1.seconds)

        val firstWorkRequest = workRequestFactory.createWorkRequest(taskId, firstRequest, BGTaskManagerConfig())
        taskScheduler.scheduleTask(taskId, firstRequest, firstWorkRequest, ExistingWorkPolicy.KEEP)
        val firstStartTime = triggerStartTime(firstWorkRequest)

        val secondWorkRequest = workRequestFactory.createWorkRequest(taskId, secondRequest, BGTaskManagerConfig())
        taskScheduler.scheduleTask(taskId, secondRequest, secondWorkRequest, ExistingWorkPolicy.KEEP)
        val afterKeepStartTime = triggerStartTime(firstWorkRequest)

        assertEquals(firstStartTime, afterKeepStartTime)
        assertTrue(taskScheduler.isScheduled(taskId, firstRequest.schedule))
    }

    @Test
    fun replacePolicyReplacesExistingSchedule() {
        val taskId = "replace-policy-task"
        val firstRequest = oneTimeRequest(24.hours)
        val secondRequest = oneTimeRequest(1.seconds)

        val firstWorkRequest = workRequestFactory.createWorkRequest(taskId, firstRequest, BGTaskManagerConfig())
        taskScheduler.scheduleTask(taskId, firstRequest, firstWorkRequest, ExistingWorkPolicy.KEEP)
        val firstStartTime = triggerStartTime(firstWorkRequest)

        val secondWorkRequest = workRequestFactory.createWorkRequest(taskId, secondRequest, BGTaskManagerConfig())
        taskScheduler.scheduleTask(taskId, secondRequest, secondWorkRequest, ExistingWorkPolicy.REPLACE)
        val replacedStartTime = triggerStartTime(firstWorkRequest)

        assertTrue(replacedStartTime < firstStartTime)
        assertTrue(taskScheduler.isScheduled(taskId, secondRequest.schedule))
    }

    @Test
    fun cancelWorkByIdRemovesScheduledTask() {
        val taskId = "cancel-by-id-task"
        val request = oneTimeRequest(1.hours)
        val workRequest = workRequestFactory.createWorkRequest(taskId, request, BGTaskManagerConfig())

        taskScheduler.scheduleTask(taskId, request, workRequest, ExistingWorkPolicy.KEEP)
        assertTrue(taskScheduler.isScheduled(taskId, request.schedule))

        taskScheduler.cancelWorkById(workRequest.id, request.schedule)

        assertFalse(taskScheduler.isScheduled(taskId, request.schedule))
    }

    @Test
    fun cancelAllWorkByTagRemovesAllScheduledTasks() {
        val firstTask = "cancel-all-first"
        val secondTask = "cancel-all-second"
        val request = oneTimeRequest(1.hours)

        val firstWork = workRequestFactory.createWorkRequest(firstTask, request, BGTaskManagerConfig())
        val secondWork = workRequestFactory.createWorkRequest(secondTask, request, BGTaskManagerConfig())

        taskScheduler.scheduleTask(firstTask, request, firstWork, ExistingWorkPolicy.KEEP)
        taskScheduler.scheduleTask(secondTask, request, secondWork, ExistingWorkPolicy.KEEP)

        assertTrue(taskScheduler.isScheduled(firstTask, request.schedule))
        assertTrue(taskScheduler.isScheduled(secondTask, request.schedule))

        taskScheduler.cancelAllWorkByTag(WorkRequestFactory.WORK_REQUEST_TAG)

        assertFalse(taskScheduler.isScheduled(firstTask, request.schedule))
        assertFalse(taskScheduler.isScheduled(secondTask, request.schedule))
    }

    private fun oneTimeRequest(initialDelay: Duration): TaskRequest {
        return TaskRequest(
            payload = TestPayload,
            schedule = TaskSchedule.OneTime(initialDelay)
        )
    }

    private fun triggerStartTime(workRequest: WorkRequest): Long {
        return scheduler.getTriggersOfJob(workRequest.jobDetail.key).single().startTime.time
    }

    private fun createInMemoryScheduler(): Scheduler {
        val properties = Properties().apply {
            put("org.quartz.scheduler.instanceName", "meeseeks-test-${UUID.randomUUID()}")
            put("org.quartz.scheduler.instanceId", "AUTO")
            put("org.quartz.threadPool.class", "org.quartz.simpl.SimpleThreadPool")
            put("org.quartz.threadPool.threadCount", "1")
            put("org.quartz.jobStore.class", "org.quartz.simpl.RAMJobStore")
        }

        return StdSchedulerFactory(properties).scheduler.apply {
            start()
        }
    }
}
