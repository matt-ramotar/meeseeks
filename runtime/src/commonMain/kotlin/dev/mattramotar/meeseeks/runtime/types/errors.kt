package dev.mattramotar.meeseeks.runtime.types


public class TransientNetworkException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)


public class PermanentValidationException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)
