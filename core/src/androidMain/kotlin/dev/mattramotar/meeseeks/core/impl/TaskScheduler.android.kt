package dev.mattramotar.meeseeks.core.impl

import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import dev.mattramotar.meeseeks.core.Task
import dev.mattramotar.meeseeks.core.TaskSchedule
import java.util.UUID

internal actual class TaskScheduler(
    private val workManager: WorkManager
) {
    actual fun scheduleTask(
        taskId: Long,
        task: Task,
        workRequest: WorkRequest,
        existingWorkPolicy: ExistingWorkPolicy
    ) {
        when (task.schedule) {
            is TaskSchedule.OneTime -> {
                workManager.enqueueUniqueWork(
                    WorkRequestFactory.uniqueWorkNameFor(taskId),
                    existingWorkPolicy.asAndroidExistingWorkPolicy(),
                    workRequest.delegate as OneTimeWorkRequest
                )
            }

            is TaskSchedule.Periodic -> {
                val periodicPolicy = when (existingWorkPolicy) {
                    ExistingWorkPolicy.KEEP -> ExistingPeriodicWorkPolicy.KEEP
                    else -> ExistingPeriodicWorkPolicy.UPDATE
                }
                workManager.enqueueUniquePeriodicWork(
                    WorkRequestFactory.uniqueWorkNameFor(taskId),
                    periodicPolicy,
                    workRequest.delegate as PeriodicWorkRequest
                )
            }
        }
    }

    actual fun isScheduled(taskId: Long): Boolean {
        val infos = workManager
            .getWorkInfosForUniqueWork(WorkRequestFactory.uniqueWorkNameFor(taskId))
            .get()

        return infos.isNotEmpty() && infos.any { !it.state.isFinished }
    }

    actual fun cancelWorkById(schedulerId: String) {
        workManager.cancelWorkById(UUID.fromString(schedulerId))
    }

    actual fun cancelUniqueWork(uniqueWorkName: String) {
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