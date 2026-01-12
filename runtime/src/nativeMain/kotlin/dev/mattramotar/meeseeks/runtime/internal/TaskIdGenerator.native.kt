package dev.mattramotar.meeseeks.runtime.internal

import platform.Foundation.NSUUID

internal actual fun generateTaskIdValue(): String = NSUUID().UUIDString
