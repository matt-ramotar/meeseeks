package dev.mattramotar.meeseeks.runtime.impl.concurrency

internal actual inline fun <R> synchronized(lock: Any, block: () -> R): R =
    kotlin.synchronized(lock, block)