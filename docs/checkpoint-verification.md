# Checkpoint Kill-Resume Verification

Meeseeks verifies checkpoint resume behavior with a repeatable JVM fixture in `CheckpointPersistenceJvmTest.multiStepWorkerSkipsCompletedStepsAfterManagerRestart`.

The fixture models a worker extending `CheckpointedWorker` with three idempotent steps:

1. `download`
2. `transform`
3. `upload`

On the first execution, the worker completes `download` and `transform`, writes a checkpoint after each completed step, then returns a transient failure to model process or manager death after durable progress was saved. The second execution builds a new worker registry and worker instance against the same Meeseeks database. The restarted worker reads the checkpoint and runs only `upload`.

The assertion is intentionally about side effects, not just stored rows: `download`, `transform`, and `upload` each appear exactly once. Completed steps are not repeated after restart/resume, and the checkpoint row is cleared after final success.

Run the proof with:

```bash
./gradlew :runtime:jvmTest --stacktrace
```

The same gate also covers checkpoint overwrite, key clearing, corrupt payload errors, incompatible payload errors, retry retention, permanent failure cleanup, and cancellation cleanup.

## Platform Limits

This proof is platform-independent at the runtime contract level: it exercises Meeseeks persistence, worker recreation, checkpoint read/write, retry retention, and terminal cleanup without relying on a specific OS scheduler.

It does not claim exact process-kill scheduling guarantees for every target:

- Android execution still depends on WorkManager delivery and OS constraints.
- iOS background execution remains best effort and depends on the system granting runtime.
- JS execution depends on the available database driver and browser/runtime lifecycle.
- JVM execution is the strongest automated proof because the test can synchronously run the shared `TaskExecutor` against the persistent database.

## Worker Guidance

Place checkpoints after an idempotent step has finished and its side effect is safe to skip on a later retry. Do not checkpoint before starting a side effect; a crash after the checkpoint but before the side effect would incorrectly skip work.

Recommended pattern:

```kotlin
val checkpoint = checkpoints.read<SyncCheckpoint>() ?: SyncCheckpoint()

if (!checkpoint.downloaded) {
    downloadBatch()
    checkpoints.write(checkpoint.copy(downloaded = true))
}

if (!checkpoint.transformed) {
    transformBatch()
    checkpoints.write(checkpoint.copy(transformed = true))
}

uploadBatch()
```

Workers should treat checkpoint data as app-owned schema. If `CheckpointDecodeException` or `CheckpointIncompatibleException` is safe to recover from, clear the checkpoint and restart from step zero. If replaying a step would duplicate external side effects, fail explicitly and let the app repair or migrate the stored checkpoint state.
