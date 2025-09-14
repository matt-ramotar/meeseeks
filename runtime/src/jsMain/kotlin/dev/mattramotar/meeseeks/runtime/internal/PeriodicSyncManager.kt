package dev.mattramotar.meeseeks.runtime.internal

import kotlin.js.Promise

internal external interface PeriodicSyncManager {
    fun register(tag: String, options: dynamic): Promise<Unit>
    fun getTags(): Promise<Array<String>>
    fun unregister(tag: String): Promise<Boolean>
}