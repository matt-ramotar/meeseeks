package dev.mattramotar.meeseeks.runtime

/**
 * Fixed identifiers for iOS BackgroundTasks.
 * Developers must register these in their Info.plist under `BGTaskSchedulerPermittedIdentifiers`.
 */
object BGTaskIdentifiers {

    /**
     * Identifier for short-running tasks (BGAppRefreshTask).
     */
    const val REFRESH = "dev.mattramotar.meeseeks.task.refresh"

    /**
     * Identifier for tasks requiring network or charging (BGProcessingTask).
     */
    const val PROCESSING = "dev.mattramotar.meeseeks.task.processing"


}