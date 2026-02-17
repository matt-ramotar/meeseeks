package dev.mattramotar.meeseeks.runtime

/**
 * Responsible for creating a specific type of [Worker].
 */
public fun interface WorkerFactory<T : TaskPayload> {
    /**
     * Creates an instance of the [Worker] specialized for the provided [definition].
     */
    public fun create(
        appContext: AppContext,
    ): Worker<T>
}
