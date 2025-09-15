package dev.mattramotar.meeseeks.runtime.telemetry

/**
 * No-op logger for testing or disabling logging.
 */
object NoOpLogger : Logger {
    override fun log(level: LogLevel, message: String) {}
    override fun logStructured(level: LogLevel, tag: String, data: String) {}
    override fun error(message: String, throwable: Throwable?) {}
    override fun isEnabled(level: LogLevel): Boolean = false
}