package dev.mattramotar.meeseeks.runtime.impl.concurrency

import platform.objc.objc_sync_enter
import platform.objc.objc_sync_exit


internal actual inline fun <R> synchronized(lock: Any, block: () -> R): R {
    objc_sync_enter(lock)
    return try {
        block()
    } finally {
        objc_sync_exit(lock)
    }
}