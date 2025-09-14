package dev.mattramotar.meeseeks.runtime.internal.concurrency

actual inline fun <R> synchronized(lock: Any, block: () -> R): R {
    // Single threaded
    return block()
}