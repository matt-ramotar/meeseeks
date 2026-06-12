package dev.mattramotar.meeseeks.runtime.internal

import dev.mattramotar.meeseeks.runtime.TaskRequest
import dev.mattramotar.meeseeks.runtime.TaskSchedule
import dev.mattramotar.meeseeks.runtime.internal.WorkRequestFactory.Companion.JOB_GROUP
import org.quartz.JobKey
import org.quartz.ObjectAlreadyExistsException
import org.quartz.Scheduler
import org.quartz.impl.matchers.GroupMatcher

internal actual class TaskScheduler(
    private val scheduler: Scheduler
) {

    actual fun scheduleTask(
        taskId: String,
        task: TaskRequest,
        workRequest: WorkRequest,
        existingWorkPolicy: ExistingWorkPolicy
    ) {
        val jobDetail = workRequest.jobDetail
        val triggers = workRequest.triggers
        val jobKey = jobDetail.key

        val jobAlreadyExists = scheduler.checkExists(jobKey)

        when (existingWorkPolicy) {
            ExistingWorkPolicy.REPLACE -> {
                if (jobAlreadyExists) {
                    scheduler.deleteJob(jobKey)
                }
                // replace = true keeps the store atomic when another path
                // stores the same unique work between deleteJob and this call.
                scheduler.scheduleJob(jobDetail, triggers.toSet(), true)
            }

            ExistingWorkPolicy.KEEP -> {
                if (!jobAlreadyExists) {
                    try {
                        scheduler.scheduleJob(jobDetail, triggers.toSet(), false)
                    } catch (_: ObjectAlreadyExistsException) {
                        // Another path (e.g. the startup rescheduler) stored the
                        // same unique work between checkExists and scheduleJob.
                        // KEEP semantics: the existing job wins.
                    }
                }
            }
        }
    }

    actual fun isScheduled(taskId: String, taskSchedule: TaskSchedule): Boolean {
        val jobKey = JobKey(
            WorkRequestFactory.uniqueWorkNameFor(taskId, taskSchedule),
            JOB_GROUP
        )
        return scheduler.checkExists(jobKey)
    }

    actual fun cancelWorkById(schedulerId: String, taskSchedule: TaskSchedule) {
        val jobKey = JobKey(schedulerId, JOB_GROUP)
        scheduler.deleteJob(jobKey)
    }

    actual fun cancelUniqueWork(uniqueWorkName: String, taskSchedule: TaskSchedule) {
        val jobKey = JobKey(uniqueWorkName, JOB_GROUP)
        scheduler.deleteJob(jobKey)
    }

    actual fun cancelAllWorkByTag(tag: String) {
        val jobKeys = scheduler.getJobKeys(GroupMatcher.jobGroupEquals(tag))
        scheduler.deleteJobs(jobKeys.toList())
    }
}
