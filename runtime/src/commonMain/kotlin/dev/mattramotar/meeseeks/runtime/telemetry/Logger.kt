package dev.mattramotar.meeseeks.runtime.telemetry

/**
 * Generic logger interface for structured telemetry output.
 * Implementations may write to console, files, remote services.
 */
interface Logger {
    /**
     * Log a message at the specified level.
     */
    fun log(level: LogLevel, message: String)

    /**
     * Log structured data at the specified level.
     */
    fun logStructured(level: LogLevel, tag: String, data: String)

    /**
     * Log an error with an optional throwable.
     */
    fun error(message: String, throwable: Throwable? = null)

    /**
     * Check if a log level is enabled.
     */
    fun isEnabled(level: LogLevel): Boolean

    /**
     * Flush any buffered logs.
     */
    fun flush() {}
}