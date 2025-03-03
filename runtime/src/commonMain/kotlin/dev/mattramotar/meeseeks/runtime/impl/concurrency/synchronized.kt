package dev.mattramotar.meeseeks.runtime.impl.concurrency


internal expect inline fun <R> synchronized(lock: Any, block: () -> R): R