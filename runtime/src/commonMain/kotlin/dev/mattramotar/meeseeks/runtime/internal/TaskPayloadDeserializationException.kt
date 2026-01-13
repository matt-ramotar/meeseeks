package dev.mattramotar.meeseeks.runtime.internal

internal class TaskPayloadDeserializationException(
    val typeId: String,
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)
