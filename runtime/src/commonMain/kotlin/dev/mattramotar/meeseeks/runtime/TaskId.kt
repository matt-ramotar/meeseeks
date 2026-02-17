package dev.mattramotar.meeseeks.runtime

import kotlin.jvm.JvmInline


/**
 * Unique identifier of a [Worker], backed by a UUID string.
 */
@JvmInline
public value class TaskId(public val value: String)
