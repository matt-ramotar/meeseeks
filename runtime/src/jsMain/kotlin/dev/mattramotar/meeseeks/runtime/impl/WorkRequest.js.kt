package dev.mattramotar.meeseeks.runtime.impl

internal actual class WorkRequest(
    taskId: Long
) {
    actual val id: String = taskId.toString()
}