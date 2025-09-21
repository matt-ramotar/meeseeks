package dev.mattramotar.meeseeks.runtime.internal

import dev.mattramotar.meeseeks.runtime.BGTaskManagerConfig
import dev.mattramotar.meeseeks.runtime.TaskRequest
import dev.mattramotar.meeseeks.runtime.TaskSchedule
import org.quartz.JobBuilder
import org.quartz.JobKey
import org.quartz.SimpleScheduleBuilder.simpleSchedule
import org.quartz.Trigger
import org.quartz.TriggerBuilder
import java.util.*


internal actual class WorkRequestFactory {

    actual fun createWorkRequest(
        taskId: Long,
        taskRequest: TaskRequest,
        config: BGTaskManagerConfig
    ): WorkRequest {
        return createWorkRequest(taskId, taskRequest, null)
    }

    /**
     * Always uses [buildOneTimeTrigger]. See: [#21](https://github.com/matt-ramotar/meeseeks/issues/21).
     */
    internal fun createWorkRequest(
        taskId: Long,
        taskRequest: TaskRequest,
        delayOverrideMs: Long?,
    ): WorkRequest {
        val jobKey = JobKey(uniqueWorkNameFor(taskId, taskRequest.schedule), JOB_GROUP)

        val jobDetail = JobBuilder.newJob(BGTaskQuartzJob::class.java)
            .withIdentity(jobKey)
            .usingJobData(KEY_TASK_ID, taskId)
            .storeDurably(true)
            .build()

        val initialDelayMs = delayOverrideMs ?: when (taskRequest.schedule) {
            is TaskSchedule.OneTime -> taskRequest.schedule.initialDelay.inWholeMilliseconds
            is TaskSchedule.Periodic -> taskRequest.schedule.initialDelay.inWholeMilliseconds
        }

        val trigger = buildOneTimeTrigger(initialDelayMs)
        return WorkRequest(jobDetail, listOf(trigger))
    }

    private fun buildOneTimeTrigger(delayMillis: Long): Trigger {
        val startAt = Date(System.currentTimeMillis() + delayMillis)
        return TriggerBuilder.newTrigger()
            .startAt(startAt)
            .withSchedule(simpleSchedule().withRepeatCount(0))
            .build()
    }

    actual companion object {
        private const val KEY_TASK_ID = "task_id"

        actual fun uniqueWorkNameFor(taskId: Long, taskSchedule: TaskSchedule): String {
            return "meeseeks_work_$taskId"
        }

        actual fun taskIdFrom(uniqueWorkName: String, taskSchedule: TaskSchedule): Long {
            return uniqueWorkName.removePrefix("meeseeks_work_").toLong()
        }

        actual val WORK_REQUEST_TAG: String = JOB_GROUP

        const val JOB_GROUP = "meeseeks"
    }
}
