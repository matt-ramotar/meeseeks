package dev.mattramotar.meeseeks.runtime

/**
 * Abstract task worker.
 */
abstract class Worker<T : DynamicData>(
    val appContext: AppContext,
) {
    abstract suspend fun run(data: T, context: RuntimeContext): TaskResult
}

