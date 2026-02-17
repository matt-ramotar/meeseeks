package dev.mattramotar.meeseeks.runtime

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

class TaskRequestPeriodicBuilderTest {

    private object TestPayload : TaskPayload

    @Test
    fun periodicBuilderMapsIntervalWithoutChangingInitialDelayOrFlexWindow() {
        val interval = 5.minutes

        val request = TaskRequest.periodic(TestPayload, interval)
        val schedule = request.schedule as TaskSchedule.Periodic

        assertEquals(Duration.ZERO, schedule.initialDelay)
        assertEquals(interval, schedule.interval)
        assertEquals(Duration.ZERO, schedule.flexWindow)
    }

    @Test
    fun periodicBuilderPreservesNonDefaultIntervalValue() {
        val interval = 17.minutes

        val request = TaskRequest.periodic(TestPayload, interval)
        val schedule = request.schedule as TaskSchedule.Periodic

        assertEquals(interval, schedule.interval)
    }
}
