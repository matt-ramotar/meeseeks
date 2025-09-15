package dev.mattramotar.meeseeks.runtime.telemetry

/**
 * Default console logger implementation.
 */
class ConsoleLogger(
    private val minLevel: LogLevel = LogLevel.INFO,
    private val enableColors: Boolean = true
) : Logger {

    override fun log(level: LogLevel, message: String) {
        if (level >= minLevel) {
            val prefix = formatPrefix(level)
            println("$prefix $message")
        }
    }

    override fun logStructured(level: LogLevel, tag: String, data: String) {
        if (level >= minLevel) {
            val prefix = formatPrefix(level)
            println("$prefix [$tag] $data")
        }
    }

    override fun error(message: String, throwable: Throwable?) {
        val prefix = formatPrefix(LogLevel.ERROR)
        println("$prefix $message")
        throwable?.let {
            println("$prefix ${it.stackTraceToString()}")
        }
    }

    override fun isEnabled(level: LogLevel): Boolean = level >= minLevel

    private fun formatPrefix(level: LogLevel): String {
        return when (level) {
            LogLevel.DEBUG -> if (enableColors) "\u001B[36m[DEBUG]\u001B[0m" else "[DEBUG]"
            LogLevel.INFO -> if (enableColors) "\u001B[32m[INFO]\u001B[0m" else "[INFO]"
            LogLevel.WARN -> if (enableColors) "\u001B[33m[WARN]\u001B[0m" else "[WARN]"
            LogLevel.ERROR -> if (enableColors) "\u001B[31m[ERROR]\u001B[0m" else "[ERROR]"
        }
    }
}