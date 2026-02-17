package dev.mattramotar.meeseeks.runtime

/**
 * Fixed identifiers for iOS BackgroundTasks.
 * Developers must register these in their Info.plist under `BGTaskSchedulerPermittedIdentifiers`.
 */
public object BGTaskIdentifiers {

    /**
     * Identifier for short-running tasks (BGAppRefreshTask).
     */
    public const val REFRESH: String = "dev.mattramotar.meeseeks.task.refresh"

    /**
     * Identifier for tasks requiring network or charging (BGProcessingTask).
     */
    public const val PROCESSING: String = "dev.mattramotar.meeseeks.task.processing"
}
