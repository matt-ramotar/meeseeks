package dev.mattramotar.meeseeks.runtime.internal

import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import dev.mattramotar.meeseeks.runtime.TaskRequest
import dev.mattramotar.meeseeks.runtime.TaskSchedule
import java.util.*

internal actual class TaskScheduler(
    private val workManager: WorkManager
) {

    /**
     * All work is [OneTimeWorkRequest]. See: [#21](https://github.com/matt-ramotar/meeseeks/issues/21).
     */
    actual fun scheduleTask(
        taskId: Long,
        task: TaskRequest,
        workRequest: WorkRequest,
        existingWorkPolicy: ExistingWorkPolicy
    ) {
        workManager.enqueueUniqueWork(
            WorkRequestFactory.uniqueWorkNameFor(taskId, task.schedule),
            existingWorkPolicy.asAndroidExistingWorkPolicy(),
            workRequest.delegate as OneTimeWorkRequest
        )
    }

    actual fun isScheduled(taskId: Long, taskSchedule: TaskSchedule): Boolean {
        val infos = workManager
            .getWorkInfosForUniqueWork(WorkRequestFactory.uniqueWorkNameFor(taskId, taskSchedule))
            .get()

        return infos.isNotEmpty() && infos.any { !it.state.isFinished }
    }

    actual fun cancelWorkById(schedulerId: String, taskSchedule: TaskSchedule) {
        workManager.cancelWorkById(UUID.fromString(schedulerId))
    }

    actual fun cancelUniqueWork(uniqueWorkName: String, taskSchedule: TaskSchedule) {
        workManager.cancelUniqueWork(uniqueWorkName)
    }

    actual fun cancelAllWorkByTag(tag: String) {
        workManager.cancelAllWorkByTag(tag)
    }

    private fun ExistingWorkPolicy.asAndroidExistingWorkPolicy(): androidx.work.ExistingWorkPolicy =
        when (this) {
            ExistingWorkPolicy.REPLACE -> androidx.work.ExistingWorkPolicy.REPLACE
            ExistingWorkPolicy.KEEP -> androidx.work.ExistingWorkPolicy.KEEP
        }
}