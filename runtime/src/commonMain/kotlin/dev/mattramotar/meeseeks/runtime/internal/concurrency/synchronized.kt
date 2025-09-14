package dev.mattramotar.meeseeks.runtime.internal.concurrency


internal expect inline fun <R> synchronized(lock: Any, block: () -> R): R