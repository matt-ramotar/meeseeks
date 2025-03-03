package dev.mattramotar.meeseeks.runtime.impl

import kotlinx.datetime.Clock

object Timestamp {
    fun now(): Long = Clock.System.now().toEpochMilliseconds()
}