package dev.mattramotar.meeseeks.core.impl.concurrency

import kotlin.concurrent.AtomicInt
import kotlin.concurrent.AtomicReference


private val spinLocks = AtomicReference<Map<Any, SpinLock>>(emptyMap())

internal actual inline fun <R> synchronized(lock: Any, block: () -> R): R {
    val spinLock = findOrCreateSpinLock(lock)

    spinLock.lock()
    try {
        return block()
    } finally {
        spinLock.unlock()
    }
}

private fun findOrCreateSpinLock(key: Any): SpinLock {
    while (true) {
        val existingSpinLocks = spinLocks.value
        val existingSpinLock = existingSpinLocks[key]
        if (existingSpinLock != null) return existingSpinLock

        val newSpinLock = SpinLock()
        val newSpinLocks = existingSpinLocks + (key to newSpinLock)
        if (spinLocks.compareAndSet(existingSpinLocks, newSpinLocks)) {
            return newSpinLock
        }
    }
}

internal class SpinLock {
    private val locked = AtomicInt(0)

    fun lock() {
        while (true) {
            if (locked.compareAndSet(0, 1)) return
        }
    }

    fun unlock() {
        locked.value = 0
    }
}
