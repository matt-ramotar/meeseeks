package dev.mattramotar.meeseeks.runtime.internal

internal actual class WorkRequest(
    taskId: Long
) {
    actual val id: String = taskId.toString()
}