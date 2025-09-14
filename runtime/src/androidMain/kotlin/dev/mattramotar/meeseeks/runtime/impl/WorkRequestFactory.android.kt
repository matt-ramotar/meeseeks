package dev.mattramotar.meeseeks.runtime.impl

import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.workDataOf
import dev.mattramotar.meeseeks.runtime.TaskRequest
import dev.mattramotar.meeseeks.runtime.TaskRetryPolicy
import dev.mattramotar.meeseeks.runtime.TaskSchedule
import java.util.concurrent.TimeUnit

internal actual class WorkRequestFactory(
    private val minBackoffMillis: Long = 10_000
) {
    actual fun createWorkRequest(
        taskId: Long,
        taskRequest: TaskRequest
    ): WorkRequest {

        val constraints = buildConstraints(taskRequest)

        val (backoffPolicy, backoffDelay) = buildBackoffPolicy(taskRequest.retryPolicy)

        val inputData = workDataOf(
            KEY_TASK_ID to taskId,
            KEY_DYNAMIC_DATA to taskRequest.data
        )

        val delegateWorkRequest = when (val schedule = taskRequest.schedule) {
            is TaskSchedule.OneTime -> {
                OneTimeWorkRequestBuilder<BGTaskCoroutineWorker>()
                    .setConstraints(constraints)
                    .setBackoffCriteria(backoffPolicy, backoffDelay, TimeUnit.MILLISECONDS)
                    .setInputData(inputData)
                    .addTag(WORK_REQUEST_TAG)
                    .build()
            }

            is TaskSchedule.Periodic -> {

                val intervalMillis = schedule.interval.inWholeMilliseconds
                val flexMillis = schedule.flexWindow.inWholeMilliseconds
                val repeatInterval = intervalMillis.coerceAtLeast(MINIMUM_PERIODIC_INTERVAL_MS)
                val flexInterval = if (flexMillis > 0) flexMillis else repeatInterval

                PeriodicWorkRequestBuilder<BGTaskCoroutineWorker>(
                    repeatInterval, TimeUnit.MILLISECONDS,
                    flexInterval, TimeUnit.MILLISECONDS
                )
                    .setConstraints(constraints)
                    .setBackoffCriteria(backoffPolicy, backoffDelay, TimeUnit.MILLISECONDS)
                    .setInputData(inputData)
                    .addTag(WORK_REQUEST_TAG)
                    .build()
            }
        }

        return WorkRequest(delegateWorkRequest)
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

    private fun buildBackoffPolicy(policy: TaskRetryPolicy): Pair<BackoffPolicy, Long> {
        return when (policy) {
            is TaskRetryPolicy.ExponentialBackoff -> {

                BackoffPolicy.EXPONENTIAL to policy.initialInterval
                    .inWholeMilliseconds
                    .coerceAtLeast(minBackoffMillis)
            }

            is TaskRetryPolicy.FixedInterval -> {
                BackoffPolicy.LINEAR to policy.retryInterval
                    .inWholeMilliseconds
                    .coerceAtLeast(minBackoffMillis)
            }
        }
    }

    actual companion object {

        const val KEY_TASK_ID = "task_id"
        const val KEY_MEESEEKS_TYPE = "meeseeks_type"
        const val KEY_DYNAMIC_DATA = "dynamic_data"
        private const val MINIMUM_PERIODIC_INTERVAL_MS = 15 * 60 * 1000L
        private const val UNIQUE_WORK_NAME_PREFIX = "meeseeks_work_"

        actual fun uniqueWorkNameFor(taskId: Long, taskSchedule: TaskSchedule): String {
            return UNIQUE_WORK_NAME_PREFIX + taskId
        }

        actual fun taskIdFrom(uniqueWorkName: String, taskSchedule: TaskSchedule): Long {
            return uniqueWorkName.removePrefix(UNIQUE_WORK_NAME_PREFIX).toLong()
        }

        actual val WORK_REQUEST_TAG: String = "meeseeks"
    }
}