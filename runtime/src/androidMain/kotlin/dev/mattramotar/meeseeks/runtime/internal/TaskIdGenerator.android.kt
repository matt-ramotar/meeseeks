package dev.mattramotar.meeseeks.runtime.internal

import java.util.UUID

internal actual fun generateTaskIdValue(): String = UUID.randomUUID().toString()
