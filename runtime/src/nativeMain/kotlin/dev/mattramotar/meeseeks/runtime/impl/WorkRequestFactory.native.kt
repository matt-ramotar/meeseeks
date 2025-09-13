package dev.mattramotar.meeseeks.runtime.impl

import dev.mattramotar.meeseeks.runtime.BGTaskIdentifiers
import dev.mattramotar.meeseeks.runtime.Task
import dev.mattramotar.meeseeks.runtime.TaskSchedule

internal actual class WorkRequestFactory {
    actual fun createWorkRequest(
        taskId: Long,
        task: Task
    ): WorkRequest {
        val bgTaskIdentifier = bgTaskIdentifierFor(taskId, task.schedule)
        return WorkRequest(bgTaskIdentifier)
    }

    actual companion object {

        actual fun uniqueWorkNameFor(taskId: Long, taskSchedule: TaskSchedule): String {
            return bgTaskIdentifierFor(taskId, taskSchedule)
        }

        actual fun taskIdFrom(uniqueWorkName: String, taskSchedule: TaskSchedule): Long {
            return taskIdFromBGTaskIdentifier(uniqueWorkName, taskSchedule)
        }

        fun taskIdFromBGTaskIdentifier(bgTaskIdentifier: String, taskSchedule: TaskSchedule): Long {
            return when (taskSchedule) {
                is TaskSchedule.OneTime -> bgTaskIdentifier.removePrefix("${BGTaskIdentifiers.ONE_TIME_TASK}-")
                is TaskSchedule.Periodic -> bgTaskIdentifier.removePrefix("${BGTaskIdentifiers.PERIODIC_TASK}-")
            }.toLong()
        }

        fun bgTaskIdentifierFor(taskId: Long, taskSchedule: TaskSchedule): String {
            val prefix = when (taskSchedule) {
                is TaskSchedule.OneTime -> BGTaskIdentifiers.ONE_TIME_TASK
                is TaskSchedule.Periodic -> BGTaskIdentifiers.PERIODIC_TASK
            }
            return "$prefix-$taskId"
        }

        actual val WORK_REQUEST_TAG: String = "meeseeks"
    }

}