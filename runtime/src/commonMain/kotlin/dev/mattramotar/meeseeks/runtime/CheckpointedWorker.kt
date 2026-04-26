package dev.mattramotar.meeseeks.runtime

/**
 * Worker capability for resumable app-level progress.
 *
 * Existing workers keep using [Worker.run]. Workers that also implement this
 * interface receive a [CheckpointStore] when Meeseeks executes them.
 */
public interface CheckpointedWorker<T : TaskPayload> {
    public suspend fun run(
        payload: T,
        context: RuntimeContext,
        checkpoints: CheckpointStore,
    ): TaskResult
}
