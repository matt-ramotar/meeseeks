package dev.mattramotar.meeseeks.runtime

/**
 * Abstract task worker.
 */
public abstract class Worker<T : TaskPayload>(
    public val appContext: AppContext,
) {
    public abstract suspend fun run(payload: T, context: RuntimeContext): TaskResult
}
