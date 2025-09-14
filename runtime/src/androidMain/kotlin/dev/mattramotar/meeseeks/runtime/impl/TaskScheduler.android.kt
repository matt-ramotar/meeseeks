package dev.mattramotar.meeseeks.runtime.impl

import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import dev.mattramotar.meeseeks.runtime.TaskRequest
import dev.mattramotar.meeseeks.runtime.TaskSchedule
import java.util.*

internal actual class TaskScheduler(
    private val workManager: WorkManager
) {
    actual fun scheduleTask(
        taskId: Long,
        task: TaskRequest,
        workRequest: WorkRequest,
        existingWorkPolicy: ExistingWorkPolicy
    ) {
        when (task.schedule) {
            is TaskSchedule.OneTime -> {
                workManager.enqueueUniqueWork(
                    WorkRequestFactory.uniqueWorkNameFor(taskId, task.schedule),
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
                    WorkRequestFactory.uniqueWorkNameFor(taskId, task.schedule),
                    periodicPolicy,
                    workRequest.delegate as PeriodicWorkRequest
                )
            }
        }
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