package dev.mattramotar.meeseeks.runtime.internal

import dev.mattramotar.meeseeks.runtime.BGTaskIdentifiers
import kotlinx.cinterop.ExperimentalForeignApi
import platform.BackgroundTasks.BGTask
import platform.BackgroundTasks.BGTaskScheduler
import platform.Foundation.NSLog

/**
 * Registers the fixed identifiers with the OS during initialization.
 */
@OptIn(ExperimentalForeignApi::class)
internal class BGTaskRegistry(
    private val scheduler: BGTaskScheduler,
    private val coordinator: NativeTaskCoordinator
) {

    fun registerHandlers() {
        register(BGTaskIdentifiers.REFRESH)
        register(BGTaskIdentifiers.PROCESSING)
    }

    private fun register(identifier: String) {

        // We can use the default background queue
        val usingQueue = null

        val success = scheduler.registerForTaskWithIdentifier(
            identifier = identifier,
            usingQueue = usingQueue
        ) { task: BGTask? ->
            if (task != null) {

                // When the OS launches the task, hand it off to the coordinator
                coordinator.coordinateExecution(task)
            }
        }

        if (!success) {
            NSLog("[Meeseeks] Failed to register BGTaskScheduler identifier: $identifier. Ensure it is listed in Info.plist.")
        }
    }
}