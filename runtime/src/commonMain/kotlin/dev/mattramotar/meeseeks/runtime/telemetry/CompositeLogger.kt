package dev.mattramotar.meeseeks.runtime.telemetry

/**
 * Composite logger that delegates to multiple loggers.
 */
class CompositeLogger(private val loggers: List<Logger>) : Logger {
    override fun log(level: LogLevel, message: String) {
        loggers.forEach { it.log(level, message) }
    }

    override fun logStructured(level: LogLevel, tag: String, data: String) {
        loggers.forEach { it.logStructured(level, tag, data) }
    }

    override fun error(message: String, throwable: Throwable?) {
        loggers.forEach { it.error(message, throwable) }
    }

    override fun isEnabled(level: LogLevel): Boolean =
        loggers.any { it.isEnabled(level) }

    override fun flush() {
        loggers.forEach { it.flush() }
    }
}