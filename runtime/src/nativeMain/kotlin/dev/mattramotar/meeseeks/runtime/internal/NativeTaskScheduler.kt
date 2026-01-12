package dev.mattramotar.meeseeks.runtime.internal

import dev.mattramotar.meeseeks.runtime.BGTaskIdentifiers
import dev.mattramotar.meeseeks.runtime.TaskPreconditions
import dev.mattramotar.meeseeks.runtime.TaskSchedule
import dev.mattramotar.meeseeks.runtime.telemetry.Telemetry
import dev.mattramotar.meeseeks.runtime.telemetry.TelemetryEvent
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import kotlinx.coroutines.runBlocking
import platform.BackgroundTasks.BGAppRefreshTaskRequest
import platform.BackgroundTasks.BGProcessingTaskRequest
import platform.BackgroundTasks.BGTaskRequest
import platform.BackgroundTasks.BGTaskScheduler
import platform.BackgroundTasks.BGTaskSchedulerErrorCodeNotPermitted
import platform.BackgroundTasks.BGTaskSchedulerErrorCodeTooManyPendingTaskRequests
import platform.BackgroundTasks.BGTaskSchedulerErrorCodeUnavailable
import platform.Foundation.NSDate
import platform.Foundation.NSError
import platform.Foundation.NSLog
import platform.Foundation.dateWithTimeIntervalSinceNow
import kotlin.time.DurationUnit

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
internal class NativeTaskScheduler(
    private val bgTaskScheduler: BGTaskScheduler,
    private val telemetry: Telemetry? = null
) {


    fun schedule(preconditions: TaskPreconditions, schedule: TaskSchedule): SubmitRequestResult {
        val bgTaskRequest = createBGTaskRequest(preconditions, schedule)
        return submitRequest(bgTaskRequest)
    }

    fun submitRequest(request: BGTaskRequest): SubmitRequestResult {
        return try {
            memScoped {
                val errorPointer = alloc<ObjCObjectVar<NSError?>>()
                val success = bgTaskScheduler.submitTaskRequest(request, errorPointer.ptr)

                if (success) {
                    SubmitRequestResult.Success
                } else {
                    val error = errorPointer.value
                    handleSubmitError(request.identifier, error)
                }
            }
        } catch (e: Throwable) {
            NSLog("[Meeseeks] Exception during submitTaskRequest ${request.identifier}: $e")
            val result = SubmitRequestResult.Failure.NonRetriable(
                errorCode = -1,
                errorDescription = "Exception: ${e.message}"
            )
            emitTelemetry(request.identifier, result)
            result
        }
    }

    private fun handleSubmitError(
        identifier: String,
        error: NSError?
    ): SubmitRequestResult.Failure {
        val errorCode = error?.code ?: -1L
        val errorDescription = error?.localizedDescription

        val result = when (errorCode) {
            BGTaskSchedulerErrorCodeUnavailable -> {
                NSLog("[Meeseeks] BGTaskScheduler unavailable for $identifier: $errorDescription")
                SubmitRequestResult.Failure.NonRetriable(
                    errorCode = errorCode.toInt(),
                    errorDescription = errorDescription
                )
            }
            BGTaskSchedulerErrorCodeTooManyPendingTaskRequests -> {
                NSLog("[Meeseeks] Too many pending task requests for $identifier: $errorDescription")
                SubmitRequestResult.Failure.Retriable(
                    errorCode = errorCode.toInt(),
                    errorDescription = errorDescription
                )
            }
            BGTaskSchedulerErrorCodeNotPermitted -> {
                NSLog("[Meeseeks] BGTask not permitted for $identifier. Check Info.plist UIBackgroundModes. Error: $errorDescription")
                SubmitRequestResult.Failure.NonRetriable(
                    errorCode = errorCode.toInt(),
                    errorDescription = errorDescription
                )
            }
            else -> {
                NSLog("[Meeseeks] Unknown BGTaskScheduler error for $identifier (code $errorCode): $errorDescription")
                SubmitRequestResult.Failure.NonRetriable(
                    errorCode = errorCode.toInt(),
                    errorDescription = errorDescription
                )
            }
        }

        emitTelemetry(identifier, result)
        return result
    }

    private fun emitTelemetry(identifier: String, result: SubmitRequestResult.Failure) {
        try {
            telemetry?.let { t ->
                runBlocking {
                    t.onEvent(
                        TelemetryEvent.TaskSubmitFailed(
                            taskIdentifier = identifier,
                            errorCode = result.errorCode,
                            errorDescription = result.errorDescription,
                            isRetriable = result is SubmitRequestResult.Failure.Retriable
                        )
                    )
                }
            }
        } catch (e: Throwable) {
            // Telemetry failure should not affect error classification
            NSLog("[Meeseeks] Failed to emit telemetry for $identifier: $e")
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
            is TaskSchedule.OneTime -> schedule.initialDelay.toDouble(DurationUnit.SECONDS)
            is TaskSchedule.Periodic -> schedule.initialDelay.toDouble(DurationUnit.SECONDS)
        }

        if (earliestDelaySeconds > 0) {
            request.earliestBeginDate = NSDate.dateWithTimeIntervalSinceNow(earliestDelaySeconds)
        }

        return request
    }
}
