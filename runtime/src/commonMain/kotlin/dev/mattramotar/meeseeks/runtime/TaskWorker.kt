package dev.mattramotar.meeseeks.runtime

/**
 * Abstract task worker.
 */
abstract class TaskWorker {
    abstract suspend fun execute(parameters: TaskParameters): TaskResult
}