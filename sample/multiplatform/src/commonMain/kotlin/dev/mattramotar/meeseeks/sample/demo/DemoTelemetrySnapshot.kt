package dev.mattramotar.meeseeks.sample.demo

public data class DemoTelemetrySnapshot(
    public val totalEvents: Int,
    public val recentEvents: List<String>,
    public val aggregatedStatistics: Map<String, String>,
    public val statisticsJson: String,
    public val eventsJson: String,
)
