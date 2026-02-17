package dev.mattramotar.meeseeks.runtime.internal.db.model

internal enum class BackoffPolicy {
    EXPONENTIAL,
    LINEAR;

    companion object {
        fun fromDbValue(value: String): BackoffPolicy = try {
            valueOf(value)
        } catch (e: IllegalArgumentException) {
            error("Unknown BackoffPolicy value: $value")
        }
    }
}

internal fun BackoffPolicy.toDbValue(): String = name