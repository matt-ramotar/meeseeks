package dev.mattramotar.meeseeks.runtime.types


class TransientNetworkException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)


class PermanentValidationException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)