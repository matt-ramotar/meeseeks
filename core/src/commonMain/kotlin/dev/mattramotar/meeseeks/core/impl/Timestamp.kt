package dev.mattramotar.meeseeks.core.impl

import kotlinx.datetime.Clock

object Timestamp {
    fun now(): Long = Clock.System.now().toEpochMilliseconds()
}