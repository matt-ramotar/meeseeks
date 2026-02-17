package dev.mattramotar.meeseeks.runtime.internal

import dev.mattramotar.meeseeks.runtime.TaskPreconditions
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFailsWith

class ConstraintCapabilitiesTest {

    @Test
    fun androidSupportsAllPreconditions() {
        ConstraintCapabilities.ANDROID.validate(
            preconditions = TaskPreconditions(
                requiresNetwork = true,
                requiresCharging = true,
                requiresBatteryNotLow = true
            ),
            operation = "schedule"
        )
    }

    @Test
    fun jvmRejectsUnsupportedRequestedPreconditions() {
        val error = assertFailsWith<IllegalArgumentException> {
            ConstraintCapabilities.JVM.validate(
                preconditions = TaskPreconditions(
                    requiresNetwork = true,
                    requiresCharging = true
                ),
                operation = "schedule"
            )
        }

        val message = error.message.orEmpty()
        assertContains(message, "JVM")
        assertContains(message, "requiresNetwork")
        assertContains(message, "requiresCharging")
    }

    @Test
    fun iosRejectsBatteryNotLow() {
        val error = assertFailsWith<IllegalArgumentException> {
            ConstraintCapabilities.IOS.validate(
                preconditions = TaskPreconditions(requiresBatteryNotLow = true),
                operation = "reschedule"
            )
        }

        val message = error.message.orEmpty()
        assertContains(message, "iOS")
        assertContains(message, "requiresBatteryNotLow")
    }

    @Test
    fun jsRejectsNetworkRequirement() {
        val error = assertFailsWith<IllegalArgumentException> {
            ConstraintCapabilities.JS.validate(
                preconditions = TaskPreconditions(requiresNetwork = true),
                operation = "schedule"
            )
        }

        val message = error.message.orEmpty()
        assertContains(message, "JS")
        assertContains(message, "requiresNetwork")
    }
}
