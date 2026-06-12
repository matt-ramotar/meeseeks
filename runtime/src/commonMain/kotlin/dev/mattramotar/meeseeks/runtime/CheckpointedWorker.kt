package dev.mattramotar.meeseeks.runtime

/**
 * Base class for workers with resumable app-level progress.
 *
 * Subclasses receive a [CheckpointStore] when Meeseeks executes them and can
 * persist progress markers that survive retries and process restarts. Existing
 * workers keep extending [Worker]; checkpointing is opt-in via this class.
 *
 * Extending [Worker] (rather than adding a separate capability interface) ties
 * the payload type of the worker and its checkpoint-aware entry point together
 * and spares subclasses from overriding the two-argument [Worker.run], which
 * Meeseeks never invokes for checkpointed workers.
 */
public abstract class CheckpointedWorker<T : TaskPayload>(
    appContext: AppContext,
) : Worker<T>(appContext) {

    /**
     * Meeseeks always executes checkpointed workers through the
     * three-argument [run] with a [CheckpointStore]. Unit tests for a
     * checkpointed worker should call that overload with a fake store.
     */
    final override suspend fun run(payload: T, context: RuntimeContext): TaskResult =
        throw UnsupportedOperationException(
            "CheckpointedWorker executes through run(payload, context, checkpoints)."
        )

    public abstract suspend fun run(
        payload: T,
        context: RuntimeContext,
        checkpoints: CheckpointStore,
    ): TaskResult
}
