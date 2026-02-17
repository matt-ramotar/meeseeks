package dev.mattramotar.meeseeks.runtime.telemetry

/**
 * Generic logger interface for structured telemetry output.
 * Implementations may write to console, files, remote services.
 */
public interface Logger {
    /**
     * Log a message at the specified level.
     */
    public fun log(level: LogLevel, message: String)

    /**
     * Log structured data at the specified level.
     */
    public fun logStructured(level: LogLevel, tag: String, data: String)

    /**
     * Log an error with an optional throwable.
     */
    public fun error(message: String, throwable: Throwable? = null)

    /**
     * Check if a log level is enabled.
     */
    public fun isEnabled(level: LogLevel): Boolean

    /**
     * Flush any buffered logs.
     */
    public fun flush() {}
}
