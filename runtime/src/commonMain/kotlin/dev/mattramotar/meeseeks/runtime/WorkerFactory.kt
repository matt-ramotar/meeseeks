package dev.mattramotar.meeseeks.runtime

/**
 * Responsible for creating a specific type of [Worker].
 */
fun interface WorkerFactory<T: TaskPayload> {
    /**
     * Creates an instance of the [Worker] specialized for the provided [definition].
     */
    fun create(
        appContext: AppContext
    ): Worker<T>
}