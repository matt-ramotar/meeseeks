package dev.mattramotar.meeseeks.runtime.impl

import kotlin.js.Promise

internal external interface SyncManager {
    fun register(tag: String): Promise<Unit>
    fun getTags(): Promise<Array<String>>
}