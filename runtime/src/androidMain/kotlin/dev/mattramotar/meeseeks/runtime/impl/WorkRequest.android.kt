package dev.mattramotar.meeseeks.runtime.impl


internal actual class WorkRequest(
    val delegate: androidx.work.WorkRequest
) {
    actual val id: Long = delegate.id.node()
}