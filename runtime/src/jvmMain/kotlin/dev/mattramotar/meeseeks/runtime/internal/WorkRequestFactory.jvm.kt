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
        val jobKey = JobKey(uniqueWorkNameFor(taskId, taskRequest.schedule), JOB_GROUP)

        val jobDetail = JobBuilder.newJob(BGTaskQuartzJob::class.java)
            .withIdentity(jobKey)
            .usingJobData(KEY_TASK_ID, taskId)
            .build()

        val triggers: List<Trigger> = when (val schedule = taskRequest.schedule) {
            is TaskSchedule.OneTime -> {
                listOf(buildOneTimeTrigger(schedule.initialDelay.inWholeMilliseconds))
            }

            is TaskSchedule.Periodic -> {
                listOf(buildPeriodicTrigger(schedule.interval.inWholeMilliseconds))
            }
        }

        return WorkRequest(jobDetail, triggers)
    }

    private fun buildOneTimeTrigger(delayMillis: Long): Trigger {
        val startAt = Date(System.currentTimeMillis() + delayMillis)
        return TriggerBuilder.newTrigger()
            .startAt(startAt)
            .withSchedule(simpleSchedule().withRepeatCount(0))
            .build()
    }

    private fun buildPeriodicTrigger(intervalMillis: Long): Trigger {
        return TriggerBuilder.newTrigger()
            .startNow()
            .withSchedule(
                simpleSchedule()
                    .withIntervalInMilliseconds(intervalMillis)
                    .repeatForever()
            )
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
