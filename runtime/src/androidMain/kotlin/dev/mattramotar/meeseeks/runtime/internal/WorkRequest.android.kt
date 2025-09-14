package dev.mattramotar.meeseeks.runtime.internal


internal actual class WorkRequest(
    val delegate: androidx.work.WorkRequest
) {
    actual val id: String = delegate.id.toString()
}