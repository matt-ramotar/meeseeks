package dev.mattramotar.meeseeks.runtime.internal

import dev.mattramotar.meeseeks.runtime.BGTaskIdentifiers
import dev.mattramotar.meeseeks.runtime.TaskPreconditions
import dev.mattramotar.meeseeks.runtime.TaskSchedule
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import platform.BackgroundTasks.BGAppRefreshTaskRequest
import platform.BackgroundTasks.BGProcessingTaskRequest
import platform.BackgroundTasks.BGTaskRequest
import platform.BackgroundTasks.BGTaskScheduler
import platform.Foundation.NSDate
import platform.Foundation.NSError
import platform.Foundation.NSLog
import platform.Foundation.dateWithTimeIntervalSinceNow

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
internal class NativeTaskScheduler(
    private val bgTaskScheduler: BGTaskScheduler
) {

    fun schedule(preconditions: TaskPreconditions, schedule: TaskSchedule) {
        val bgTaskRequest = createBGTaskRequest(preconditions, schedule)
        submitRequest(bgTaskRequest)
    }

    fun submitRequest(request: BGTaskRequest) {
        try {
            memScoped {
                val errorPointer = alloc<ObjCObjectVar<NSError?>>()
                val success = bgTaskScheduler.submitTaskRequest(request, errorPointer.ptr)
                if (!success) {
                    val error = errorPointer.value
                    NSLog("[Meeseeks] Failed to submit BGTaskRequest ${request.identifier}. Error: ${error?.localizedDescription}")
                }
            }
        } catch (e: Throwable) {
            NSLog("[Meeseeks] Exception during submitTaskRequest ${request.identifier}: $e")
        }
    }

    fun cancelAllPlatformRequests() {
        bgTaskScheduler.cancelAllTaskRequests()
    }

    internal fun createBGTaskRequest(preconditions: TaskPreconditions, schedule: TaskSchedule): BGTaskRequest {
        val requiresProcessing = preconditions.requiresCharging || preconditions.requiresNetwork

        val request = if (requiresProcessing) {
            BGProcessingTaskRequest(BGTaskIdentifiers.PROCESSING).apply {
                requiresNetworkConnectivity = preconditions.requiresNetwork
                requiresExternalPower = preconditions.requiresCharging
            }
        } else {
            BGAppRefreshTaskRequest(BGTaskIdentifiers.REFRESH)
        }

        val earliestDelaySeconds = when (schedule) {
            is TaskSchedule.OneTime -> schedule.initialDelay.inWholeSeconds.toDouble()
            is TaskSchedule.Periodic -> schedule.initialDelay.inWholeSeconds.toDouble()
        }

        if (earliestDelaySeconds > 0) {
            request.earliestBeginDate = NSDate.dateWithTimeIntervalSinceNow(earliestDelaySeconds)
        }

        return request
    }
}