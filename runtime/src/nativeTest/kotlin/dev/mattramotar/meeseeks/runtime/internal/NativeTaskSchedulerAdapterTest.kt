package dev.mattramotar.meeseeks.runtime.internal

import dev.mattramotar.meeseeks.runtime.BGTaskIdentifiers
import dev.mattramotar.meeseeks.runtime.TaskPreconditions
import dev.mattramotar.meeseeks.runtime.TaskSchedule
import kotlinx.cinterop.ExperimentalForeignApi
import platform.BackgroundTasks.BGAppRefreshTaskRequest
import platform.BackgroundTasks.BGProcessingTaskRequest
import platform.BackgroundTasks.BGTaskScheduler
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalForeignApi::class)
class NativeTaskSchedulerAdapterTest {

    private val scheduler = NativeTaskScheduler(
        bgTaskScheduler = BGTaskScheduler.sharedScheduler
    )

    @Test
    fun createBGTaskRequestUsesRefreshRequestWhenProcessingNotRequired() {
        val request = scheduler.createBGTaskRequest(
            preconditions = TaskPreconditions(),
            schedule = TaskSchedule.OneTime()
        )

        assertTrue(request is BGAppRefreshTaskRequest)
        assertEquals(BGTaskIdentifiers.REFRESH, request.identifier)
        assertNull(request.earliestBeginDate)
    }

    @Test
    fun createBGTaskRequestUsesProcessingRequestWhenChargingOrNetworkRequired() {
        val request = scheduler.createBGTaskRequest(
            preconditions = TaskPreconditions(requiresNetwork = true, requiresCharging = true),
            schedule = TaskSchedule.OneTime(initialDelay = 5.seconds)
        )

        assertTrue(request is BGProcessingTaskRequest)
        assertEquals(BGTaskIdentifiers.PROCESSING, request.identifier)
        assertTrue(request.requiresNetworkConnectivity)
        assertTrue(request.requiresExternalPower)
        assertNotNull(request.earliestBeginDate)
    }

    @Test
    fun createBGTaskRequestMapsSingleProcessingConstraintAndPeriodicInitialDelay() {
        val request = scheduler.createBGTaskRequest(
            preconditions = TaskPreconditions(requiresCharging = true),
            schedule = TaskSchedule.Periodic(
                initialDelay = 3.seconds,
                interval = 60.seconds
            )
        )

        assertTrue(request is BGProcessingTaskRequest)
        assertEquals(BGTaskIdentifiers.PROCESSING, request.identifier)
        assertFalse(request.requiresNetworkConnectivity)
        assertTrue(request.requiresExternalPower)
        assertNotNull(request.earliestBeginDate)
    }
}
