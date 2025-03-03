package dev.mattramotar.meeseeks.runtime

/**
 * Abstract task worker.
 */
abstract class MrMeeseeks {
    abstract suspend fun execute(parameters: TaskParameters): TaskResult
}