package dev.mattramotar.meeseeks.core.impl.concurrency

internal actual inline fun <R> synchronized(lock: Any, block: () -> R): R =
    kotlin.synchronized(lock, block)