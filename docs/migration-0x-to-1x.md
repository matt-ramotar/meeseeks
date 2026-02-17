# Migration Guide: 0.x to 1.x

This guide summarizes high-impact behavior and process changes introduced while preparing for `1.0.0`.

## Behavior changes

### 1) Unsupported preconditions now fail fast by target

Scheduling with unsupported preconditions now throws immediately on affected targets.

- JVM: supports none
- JS: supports none
- iOS: supports `requiresNetwork` and `requiresCharging`; rejects `requiresBatteryNotLow`
- Android: supports all three

Action:

- Audit every `TaskRequest`/DSL precondition in shared code.
- Move target-specific constraints behind platform checks where needed.

### 2) Periodic interval mapping correctness

Periodic builders now map interval correctly in runtime scheduling path.

Action:

- No action usually required.
- If you observed unexpected periodic delays in earlier builds, re-validate cadence.

## Stability and release process changes

### 3) Stronger API/ABI release gates

- Runtime API/ABI checks are part of release readiness (`jvmApiCheck`, KLib API dump/check flow).
- Internal DB enum leakage into public surface has been removed.

Action:

- Keep API dump files updated as part of intentional public API changes.

### 4) Release workflow split

- CI on `main` is snapshot-publish only.
- GA releases run via manual workflow + release environment approval.

Action:

- Use manual release workflow for non-SNAPSHOT versions.

## Recommended migration verification

Run:

```bash
./gradlew preflight --stacktrace
./gradlew :runtime:jvmTest --stacktrace
./gradlew :runtime:jsTest --stacktrace
./gradlew clean build --stacktrace
```
