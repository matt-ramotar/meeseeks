package dev.mattramotar.meeseeks.runtime

/**
 * Abstract task worker.
 */
abstract class Worker<T : TaskPayload>(
    val appContext: AppContext,
) {
    abstract suspend fun run(payload: T, context: RuntimeContext): TaskResult
}

