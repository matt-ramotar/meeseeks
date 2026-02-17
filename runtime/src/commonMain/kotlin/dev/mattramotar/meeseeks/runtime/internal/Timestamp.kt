package dev.mattramotar.meeseeks.runtime.internal

import kotlinx.datetime.Clock

internal object Timestamp {
    fun now(): Long = Clock.System.now().toEpochMilliseconds()
}