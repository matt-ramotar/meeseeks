package dev.mattramotar.meeseeks.core

/**
 * Abstract task worker.
 */
abstract class MrMeeseeks {
    abstract suspend fun execute(parameters: TaskParameters): TaskResult
}