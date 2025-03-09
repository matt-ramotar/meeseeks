package dev.mattramotar.meeseeks.runtime.impl

import org.quartz.JobDetail
import org.quartz.Trigger


internal actual class WorkRequest(
    val jobDetail: JobDetail,
    val triggers: List<Trigger>
) {
    actual val id: String
        get() = jobDetail.key.name
}
