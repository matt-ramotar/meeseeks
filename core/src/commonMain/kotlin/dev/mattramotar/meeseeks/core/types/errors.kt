package dev.mattramotar.meeseeks.core.types


class TransientNetworkException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)


class PermanentValidationException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)