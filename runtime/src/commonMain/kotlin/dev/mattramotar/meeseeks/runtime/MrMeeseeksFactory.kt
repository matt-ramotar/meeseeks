package dev.mattramotar.meeseeks.runtime

/**
 * Responsible for creating a specific type of [TaskWorker].
 */
fun interface MrMeeseeksFactory {
    /**
     * Creates an instance of the [TaskWorker] specialized for the provided [task].
     */
    fun create(task: Task): TaskWorker
}