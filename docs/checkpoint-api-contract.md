# Worker Checkpoint API Contract

This is the public contract for worker-controlled checkpoints. It defines app-level resume support: a worker can save serializable progress, then read it on a later retry or manager/app restart and skip completed steps.

Meeseeks does not restore coroutine stacks, function locals, open sockets, file handles, transactions, or arbitrary in-memory objects. Workers must make side effects idempotent and explicitly write checkpoints at safe boundaries.

## Public API shape

`RuntimeContext` is a public data class in the 1.x API surface. Adding constructor properties would change generated `copy`/`componentN` members and is not 1.x-compatible. Checkpoint support should therefore be additive through a new worker base class and store type.

`CheckpointedWorker` is an abstract class extending `Worker` rather than a separate capability interface. `Worker` is already an abstract class, so single inheritance is already spent and no flexibility is lost; the subclass relationship ties the payload type parameter of the worker and its checkpoint-aware entry point together (a separate interface would let `Worker<A>` be combined with `CheckpointedWorker<B>`), and it spares every checkpointed worker from writing a dead two-argument `run` override.

```kotlin
public abstract class CheckpointedWorker<T : TaskPayload>(
    appContext: AppContext,
) : Worker<T>(appContext) {

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

public interface CheckpointStore {
    public suspend fun <T : Any> read(
        serializer: KSerializer<T>,
        key: String = DEFAULT_KEY,
    ): T?

    public suspend fun <T : Any> write(
        value: T,
        serializer: KSerializer<T>,
        key: String = DEFAULT_KEY,
    )

    public suspend fun clear(key: String = DEFAULT_KEY)
    public suspend fun clearAll()

    public companion object {
        public const val DEFAULT_KEY: String = "default"
    }
}
```

Inline reified helpers can be added for call-site ergonomics:

```kotlin
public suspend inline fun <reified T : Any> CheckpointStore.read(
    key: String = CheckpointStore.DEFAULT_KEY,
): T?

public suspend inline fun <reified T : Any> CheckpointStore.write(
    value: T,
    key: String = CheckpointStore.DEFAULT_KEY,
)
```

Execution rule:

- Meeseeks always invokes the checkpoint-aware `run(payload, context, checkpoints)` for workers extending `CheckpointedWorker`. All platforms execute workers through the shared `TaskExecutor`, so the plain two-argument `run` is never called by the runtime; it is final and throws to surface accidental direct invocation. Unit tests for a checkpointed worker should call the three-argument `run` with a fake store.
- Existing `Worker.run(payload, context)` remains unchanged for non-checkpointed workers.

There is no `schemaVersion` parameter. A version field belongs inside the worker's own `@Serializable` checkpoint class (see below), which keeps versioning in one place that the worker actually reads back.

## Serialization and type-safety boundaries

Checkpoints are app-defined serializable values, not raw Kotlin objects.

- Serialization uses `kotlinx.serialization.KSerializer<T>`.
- The stored row records `key`, serializer/type identity, payload text, and timestamps.
- The default encoding should be JSON to match existing payload serialization. Binary encodings can be a later additive option.
- If `PayloadCipher` is configured, checkpoint payloads should be encrypted with the same cipher boundary as task payloads.
- Meeseeks guarantees durable storage and retrieval of the serialized checkpoint envelope. It does not guarantee semantic compatibility after app schema changes.

Workers own schema evolution. Recommended worker pattern:

```kotlin
@Serializable
data class SyncCheckpoint(
    val schemaVersion: Int = 1,
    val uploadedBatchIds: Set<String>,
)
```

## Namespace semantics

Checkpoint rows are scoped by:

- `taskId`
- checkpoint `key`
- payload type id
- worker registration type id

Attempt number is not part of the namespace. Retries and manager/app restarts for the same task read the same checkpoint. A newly scheduled task gets a new `TaskId` and therefore a separate checkpoint namespace.

`reschedule(taskId, updatedRequest)` keeps the same task id. If the payload or worker type changes, Meeseeks should keep the row but make reads fail with a typed incompatibility error until the worker clears or migrates the checkpoint.

## Write, load, clear, and retention behavior

Writes:

- `write` persists before it returns.
- A later write with the same `taskId` and `key` overwrites the previous checkpoint.
- Multiple keys let a worker separate independent progress markers.

Loads:

- `read` returns `null` when no checkpoint exists for the key.
- `read` throws `CheckpointDecodeException` when stored data is corrupt or cannot be decoded as the requested type.
- `read` throws `CheckpointIncompatibleException` when stored type metadata (payload, worker, or checkpoint type id) does not match the requested checkpoint.

Clears:

- `clear(key)` removes one checkpoint key for the current task.
- `clearAll()` removes every checkpoint for the current task.
- Meeseeks automatically clears all checkpoints for the task on `TaskResult.Success`, permanent failure, and explicit cancellation.
- Meeseeks retains checkpoints across `TaskResult.Retry`, transient failures that still have retries remaining, process death, and manager recreation.

Retention:

- Checkpoints are retained only while the task remains resumable.
- Once a task reaches terminal success, terminal failure, or cancellation, checkpoint rows are deleted even though task logs and terminal replay events may remain.
- Future log/task pruning can delete terminal replay rows independently of checkpoint cleanup.

## Edge cases

Retry:

- Checkpoints survive retry scheduling and are loaded on the next execution attempt.
- Workers should write after completing an idempotent step, not before starting it.

Cancellation:

- Cancelling a task clears checkpoints for that task.
- A worker that sees cancellation while running should still treat the next checkpoint write as best effort; terminal cleanup wins.

Success:

- Success clears all checkpoints for the task after the terminal event/state is persisted.

Permanent failure:

- Permanent failure clears checkpoints for the task. Workers that need diagnostics should write diagnostics elsewhere, not into resumable checkpoint storage.

Payload schema changes:

- Missing checkpoint returns `null`.
- Incompatible checkpoint schema throws a typed exception.
- Workers can catch the typed exception, clear the checkpoint, and restart from step zero when that is safe.

Missing worker registration:

- If the task payload cannot be deserialized because the worker registration is missing, Meeseeks cannot enter worker code and cannot apply app-level checkpoint logic.
- The task should fail through the existing missing-registration path; checkpoint rows remain until terminal cleanup or manual cleanup runs.

Corrupt checkpoint data:

- Decode failures are explicit and typed.
- Meeseeks should not silently drop corrupt checkpoints. The worker or app should decide whether to clear and restart.

## Implementation notes

Expected persistence shape:

```sql
CREATE TABLE taskCheckpointEntity (
    taskId TEXT NOT NULL,
    key TEXT NOT NULL,
    payloadTypeId TEXT NOT NULL,
    workerTypeId TEXT NOT NULL,
    checkpointTypeId TEXT NOT NULL,
    data TEXT NOT NULL,
    createdAtMs INTEGER NOT NULL,
    updatedAtMs INTEGER NOT NULL,
    PRIMARY KEY (taskId, key),
    FOREIGN KEY (taskId) REFERENCES taskSpec(id)
);
```

The composite primary key on `(taskId, key)` already serves `WHERE taskId = ?` lookups through its left prefix, so no separate index on `taskId` is needed.

The implementation should update SQLDelight migrations and migration verification. The worker API should be commonMain-first and work across JVM, Android, iOS/native, and JS wherever the Meeseeks database is available.

## SemVer implications

Allowed in 1.x:

- Add `CheckpointStore`.
- Add the `CheckpointedWorker` base class.
- Add checkpoint exceptions.
- Add extension helpers.
- Add database tables and migrations.

Avoid in 1.x:

- Adding constructor parameters to `RuntimeContext`.
- Adding abstract methods to `Worker` or `BGTaskManager`.
- Changing `Worker.run(payload, context)` semantics for existing workers.
- Exposing raw SQLDelight checkpoint rows as the public API.

If a future 2.0 release wants checkpoint access directly on `RuntimeContext`, it can redesign the context constructor and generated data-class members at a major-version boundary.
