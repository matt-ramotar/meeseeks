package dev.mattramotar.meeseeks.runtime.internal

import kotlin.time.Clock

internal object Timestamp {
    fun now(): Long = Clock.System.now().toEpochMilliseconds()
}