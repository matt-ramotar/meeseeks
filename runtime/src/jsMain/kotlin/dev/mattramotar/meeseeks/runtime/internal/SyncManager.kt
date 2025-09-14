package dev.mattramotar.meeseeks.runtime.internal

import kotlin.js.Promise

internal external interface SyncManager {
    fun register(tag: String): Promise<Unit>
    fun getTags(): Promise<Array<String>>
}