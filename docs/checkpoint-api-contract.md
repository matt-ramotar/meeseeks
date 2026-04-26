# Worker Checkpoint API Contract

This is the public contract for worker-controlled checkpoints. It defines app-level resume support: a worker can save serializable progress, then read it on a later retry or manager/app restart and skip completed steps.

Meeseeks does not restore coroutine stacks, function locals, open sockets, file handles, transactions, or arbitrary in-memory objects. Workers must make side effects idempotent and explicitly write checkpoints at safe boundaries.

## Public API shape

`RuntimeContext` is a public data class in the 1.x API surface. Adding constructor properties would change generated `copy`/`componentN` members and is not 1.x-compatible. Checkpoint support should therefore be additive through a new worker capability and store type.

```kotlin
public interface CheckpointedWorker<T : TaskPayload> {
    public suspend fun run(
        payload: T,
        context: RuntimeContext,
        checkpoints: CheckpointStore,
    ): TaskResult
}

public interface CheckpointStore {
    public suspend fun <T : Any> read(
        key: String = DEFAULT_KEY,
        serializer: KSerializer<T>,
    ): T?

    public suspend fun <T : Any> write(
        key: String = DEFAULT_KEY,
        value: T,
        serializer: KSerializer<T>,
        schemaVersion: Int = 1,
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
    schemaVersion: Int = 1,
)
```

Execution rule:

- If a `Worker` also implements `CheckpointedWorker`, Meeseeks calls the checkpoint-aware `run(payload, context, checkpoints)` method.
- Existing `Worker.run(payload, context)` remains unchanged for non-checkpointed workers.
- A worker may still implement ordinary `Worker.run` as a fallback, but Meeseeks should prefer the checkpoint-aware path when available.

## Serialization and type-safety boundaries

Checkpoints are app-defined serializable values, not raw Kotlin objects.

- Serialization uses `kotlinx.serialization.KSerializer<T>`.
- The stored row records `key`, `schemaVersion`, serializer/type identity, payload bytes or text, and timestamps.
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
- `read` throws `CheckpointIncompatibleException` when stored type metadata or schema metadata does not match the requested checkpoint.

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
    schemaVersion INTEGER NOT NULL,
    data TEXT NOT NULL,
    createdAtMs INTEGER NOT NULL,
    updatedAtMs INTEGER NOT NULL,
    PRIMARY KEY (taskId, key),
    FOREIGN KEY (taskId) REFERENCES taskSpec(id)
);
```

The implementation should update SQLDelight migrations and migration verification. The worker API should be commonMain-first and work across JVM, Android, iOS/native, and JS wherever the Meeseeks database is available.

## SemVer implications

Allowed in 1.x:

- Add `CheckpointStore`.
- Add `CheckpointedWorker`.
- Add checkpoint exceptions.
- Add extension helpers.
- Add database tables and migrations.

Avoid in 1.x:

- Adding constructor parameters to `RuntimeContext`.
- Adding abstract methods to `Worker` or `BGTaskManager`.
- Changing `Worker.run(payload, context)` semantics for existing workers.
- Exposing raw SQLDelight checkpoint rows as the public API.

If a future 2.0 release wants checkpoint access directly on `RuntimeContext`, it can redesign the context constructor and generated data-class members at a major-version boundary.
