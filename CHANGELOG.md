# Changelog

All notable changes to Meeseeks are documented in this file.

This project follows semantic versioning.

## [Unreleased]

## [1.1.0] - 2026-06-11

### Added

- Durable terminal event replay API: `TaskEvent`, `TaskEventOutcome` (`Success`/`Failure`/`Cancelled`), `TaskEventReplay`, and `BGTaskManager` extensions `replayTerminalEvents(sinceEventId)`, `getTaskEvents(taskId)`, and `observeTaskEvents(taskId)`. Terminal outcomes are written as immutable facts at log time, so cursor-based replay survives process and app death. Delivery is at-least-once; handlers must be idempotent (#73).
- `TaskResult.Type.Cancelled`: cancellations are now persisted as durable terminal events. Consumers with an exhaustive `when` over `TaskResult.Type` need a new branch (source-level; binary compatible) (#73).
- Checkpoint persistence for resumable workers: `CheckpointedWorker` abstract class (extends `Worker`), `CheckpointStore` with suspend `read`/`write`/`clear`/`clearAll`, reified helpers, and `DEFAULT_KEY`, plus `CheckpointDecodeException` and `CheckpointIncompatibleException`. Checkpoints are retained across retries and transient failures and cleared on success, permanent failure, and cancellation (#76).
- DB migration 3: normalizes legacy unquoted `TaskResult.Type` rows, rewrites historical periodic successes to `SuccessAndScheduledNext`, and adds a `taskId` index on `taskLogEntity` (#73).
- DB migration 4: new `taskCheckpointEntity` table with composite primary key `(task_id, checkpoint_key)` and `ON DELETE CASCADE` to `taskSpec` (#76).
- Real SQLDelight migration verification: `schemaOutputDirectory` plus committed schema snapshots make `verifyCommonMainMeeseeksDatabaseMigration` test migrations against the previous schema instead of passing trivially (#73).
- Documentation: `docs/completion-replay.md` (startup cursor replay, task-scoped replay, outcome handling, platform caveats) (#74); `docs/checkpoint-api-contract.md` (checkpoint lifecycle, persistence identity, cleanup, serialization, SQL shape, edge cases) (#75); `docs/checkpoint-verification.md` (kill/resume verification model, platform limits, idempotent checkpoint placement) (#77).
- Sample: `SyncTaskManager.replayMissedCompletions` copyable startup replay helper (#74).

### Changed

- `cancel()`/`cancelAll()` now use a guarded `cancelTaskIfActive` UPDATE: cancelling an already-terminal task is a no-op — no duplicate `Cancelled` events, and a completed task keeps its terminal outcome instead of flipping to cancelled (#73).
- Periodic per-run successes are now logged as `SuccessAndScheduledNext`, never `Success`; only one-time successes log terminal `Success`. Migration 3 rewrites historical rows the same way, so pre-upgrade data cannot surface phantom terminal events (#73).

### Fixed

- JVM Quartz `TaskScheduler` check-then-act race: `KEEP` keeps the existing job instead of throwing `ObjectAlreadyExistsException` when the startup rescheduler races `schedule()`, and `REPLACE` stores atomically (#73).

### Migration notes

- Migrations 3 and 4 run automatically on first database open after upgrade. Migration 3 rewrites existing `taskLogEntity` rows and is one-way; downgrading after upgrade is not supported.
- Code that relied on `cancel()` overriding a finished task's status will now observe the preserved terminal outcome.
- Replay delivery is at-least-once; cancelling mid-flight cannot stop an in-progress attempt, which may still log a second terminal event.

## [1.0.2] - 2026-02-28

### Fixed

- Migrated runtime timestamping from `kotlinx.datetime.Clock` to `kotlin.time.Clock` and removed the `kotlinx-datetime` runtime dependency (`3334f22af6440bbded956fcc5b809f45eed4aadd`, #67).

## [1.0.1] - 2026-02-20

### Fixed

- Resolved iOS crash when logging task results by replacing `TaskResult.Type` serializer lookup in `TaskResultAdapter` with string-based encoding/decoding and compatibility handling for both JSON-quoted and plain stored values (issue #64).

## [1.0.0] - 2026-02-17

### Added

- Enforced KLib ABI validation in `:runtime` via `klibApiCheck`.
- Release workflow verification gate before Maven Central publish.
- Stable API surface and compatibility statement in docs.

### Changed

- `VERSION_NAME` and published coordinates moved from snapshot to `1.0.0`.
- Runtime module now uses strict explicit API mode.
- Preflight checks now validate real Android SDK component availability.
- `CHROME_BIN` preflight behavior is adaptive: warning when unset, hard failure only for invalid explicit paths.

### Fixed

- Removed internal `WorkerRegistration` leakage from public inline API signatures.
- iOS reschedule aggregation no longer forces both network and charging preconditions when only one is required.
- Removed unstable Kotlin compiler flag `-XXLanguage:+ImplicitSignedToUnsignedIntegerConversion`.
- Sample placeholder TODOs replaced with minimal working sample types.

## Release Notes Process

When preparing a release:

1. Move relevant entries from `Unreleased` into a new version section.
2. Keep entries grouped by `Added`, `Changed`, `Fixed`, `Removed`, `Security`.
3. Include concise migration notes when behavior changes are not backward compatible.
4. Link PRs/issues where possible.

Release notes template:

```markdown
## [<version>] - <yyyy-mm-dd>

### Added
- ...

### Changed
- ...

### Fixed
- ...

### Removed
- ...

### Security
- ...
```
