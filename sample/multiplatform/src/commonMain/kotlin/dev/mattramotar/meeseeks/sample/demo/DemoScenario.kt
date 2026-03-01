package dev.mattramotar.meeseeks.sample.demo

public enum class DemoScenario(
    public val title: String,
    public val defaultPeriodic: Boolean = false,
) {
    SUCCESS(title = "Success"),
    RETRY_THEN_SUCCESS(title = "Retry Then Success"),
    PERMANENT_FAILURE(title = "Permanent Failure"),
    PERIODIC_HEARTBEAT(title = "Periodic Heartbeat", defaultPeriodic = true);

    public companion object {
        public fun fromName(name: String): DemoScenario {
            return entries.firstOrNull { entry ->
                entry.name.equals(name, ignoreCase = true) ||
                    entry.title.equals(name, ignoreCase = true)
            } ?: SUCCESS
        }
    }
}
