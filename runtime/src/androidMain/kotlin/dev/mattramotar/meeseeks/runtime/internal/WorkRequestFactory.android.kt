package dev.mattramotar.meeseeks.runtime.internal

import android.os.Build
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.workDataOf
import dev.mattramotar.meeseeks.runtime.BGTaskManagerConfig
import dev.mattramotar.meeseeks.runtime.TaskRequest
import dev.mattramotar.meeseeks.runtime.TaskSchedule
import java.util.concurrent.TimeUnit

internal actual class WorkRequestFactory(
    private val minBackoffMillis: Long = 10_000
) {
    /**
     * Always uses [OneTimeWorkRequestBuilder]. See: [#21](https://github.com/matt-ramotar/meeseeks/issues/21).
     */
    actual fun createWorkRequest(
        taskId: String,
        taskRequest: TaskRequest,
        config: BGTaskManagerConfig
    ): WorkRequest {
        return createWorkRequest(taskId, taskRequest, config, null)
    }

    internal fun createWorkRequest(
        taskId: String,
        taskRequest: TaskRequest,
        config: BGTaskManagerConfig,
        delayOverrideMs: Long?
    ): WorkRequest {
        val constraints = buildConstraints(taskRequest)

        val inputData = workDataOf(KEY_TASK_ID to taskId)

        val builder = OneTimeWorkRequestBuilder<BGTaskCoroutineWorker>()
            .setConstraints(constraints)
            .setInputData(inputData)
            .addTag(WORK_REQUEST_TAG)

        // Determine the delay, using the override if present, the initial delay otherwise.
        val initialDelayMs = delayOverrideMs ?: when (taskRequest.schedule) {
            is TaskSchedule.OneTime -> taskRequest.schedule.initialDelay.inWholeMilliseconds
            is TaskSchedule.Periodic -> taskRequest.schedule.initialDelay.inWholeMilliseconds
        }

        if (initialDelayMs > 0) {
            builder.setInitialDelay(initialDelayMs, TimeUnit.MILLISECONDS)
        }


        if (config.allowExpedited && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
        }

        return WorkRequest(builder.build())
    }

    private fun buildConstraints(task: TaskRequest): Constraints {
        val builder = Constraints.Builder()
        if (task.preconditions.requiresNetwork) {
            builder.setRequiredNetworkType(NetworkType.CONNECTED)
        }
        if (task.preconditions.requiresCharging) {
            builder.setRequiresCharging(true)
        }
        if (task.preconditions.requiresBatteryNotLow) {
            builder.setRequiresBatteryNotLow(true)
        }
        return builder.build()
    }

    actual companion object {
        const val KEY_TASK_ID = "task_id"
        private const val UNIQUE_WORK_NAME_PREFIX = "meeseeks_work_"

        actual fun uniqueWorkNameFor(taskId: String, taskSchedule: TaskSchedule): String {
            return UNIQUE_WORK_NAME_PREFIX + taskId
        }

        actual fun taskIdFrom(uniqueWorkName: String, taskSchedule: TaskSchedule): String {
            return uniqueWorkName.removePrefix(UNIQUE_WORK_NAME_PREFIX)
        }

        actual val WORK_REQUEST_TAG: String = "meeseeks"
    }
}
