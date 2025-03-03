package dev.mattramotar.meeseeks.runtime

/**
 * Responsible for creating a specific type of [MrMeeseeks].
 */
fun interface MrMeeseeksFactory {
    /**
     * Creates an instance of the [MrMeeseeks] specialized for the provided [task].
     */
    fun create(task: Task): MrMeeseeks
}