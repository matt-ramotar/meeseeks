package dev.mattramotar.meeseeks.core.impl.concurrency


internal expect inline fun <R> synchronized(lock: Any, block: () -> R): R