# Meeseeks

![Meeseeks](.github/meeseeks.svg) 

Kotlin Multiplatform runtime for scheduling and managing background tasks across Android, JVM, JS, and iOS.

## Install

Add Meeseeks in your shared KMP source set:

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("dev.mattramotar.meeseeks:runtime:<version>")
        }
    }
}
```

## Quickstart

### 1) Define a payload and worker

```kotlin
@Serializable
data object SyncPayload : TaskPayload

class SyncWorker(appContext: AppContext) : Worker<SyncPayload>(appContext) {
    override suspend fun run(payload: SyncPayload, context: RuntimeContext): TaskResult {
        return TaskResult.Success
    }
}
```

### 2) Initialize Meeseeks and register workers

```kotlin
val manager = Meeseeks.initialize(appContext) {
    minBackoff(20.seconds)
    maxRetryCount(3)
    maxParallelTasks(5)
    allowExpedited()

    register<SyncPayload> { SyncWorker(appContext) }
}
```

### 3) Schedule tasks

```kotlin
val oneTime = manager.oneTime(SyncPayload) {
    requireNetwork()
    highPriority()
}

val periodic = manager.periodic(SyncPayload, every = 15.minutes) {
    retryWithExponentialBackoff(initialDelay = 30.seconds, maxAttempts = 5)
}
```

### 4) Observe and cancel

```kotlin
val taskId = oneTime.id
val status = manager.getTaskStatus(taskId)
val stream = manager.observeStatus(taskId)

oneTime.cancel()
```

## Platform Setup

- Android guide: `docs/platforms/android.md`
- iOS guide: `docs/platforms/ios.md`
- JS guide: `docs/platforms/js.md`
- Capability matrix: `docs/capabilities.md`
- Troubleshooting: `docs/troubleshooting.md`
- Migration notes: `docs/migration-0x-to-1x.md`

## Stable API Surface

The `1.0.0` SemVer contract is intentionally limited to these packages:

- `dev.mattramotar.meeseeks.runtime`
- `dev.mattramotar.meeseeks.runtime.dsl`
- `dev.mattramotar.meeseeks.runtime.telemetry`
- `dev.mattramotar.meeseeks.runtime.types`

Anything under `dev.mattramotar.meeseeks.runtime.internal` is implementation detail and not part of the compatibility promise.

## Contributor Prerequisites

- JDK 17
- Android SDK configured via one of:
  - `ANDROID_HOME`
  - `ANDROID_SDK_ROOT`
  - `local.properties` with `sdk.dir=/absolute/path/to/Android/sdk`
- Chrome/Chromium available for JS tests (`CHROME_BIN` optional when auto-discovery succeeds)

## Local Setup

1. Configure Android SDK (choose one approach):

```bash
export ANDROID_HOME="/absolute/path/to/Android/sdk"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
```

or create `local.properties` in the project root:

```properties
sdk.dir=/absolute/path/to/Android/sdk
```

2. Optional: configure browser binary for JS tests when auto-discovery fails:

```bash
# macOS example
export CHROME_BIN="/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"
```

```bash
# Linux example
export CHROME_BIN=/usr/bin/google-chrome
```

## Validate Your Environment

Run the preflight check before full builds:

```bash
./gradlew preflight
```

Then run the main verification commands:

```bash
./gradlew :runtime:jvmTest --stacktrace
./gradlew :runtime:jsTest --stacktrace
./gradlew clean build --stacktrace
```

For an all-in-one local validation pass:

```bash
./gradlew preflight clean build --stacktrace
```

## Release Verification Matrix

Use this exact release gate before publishing:

```bash
./gradlew preflight clean build :runtime:jvmApiCheck :runtime:klibApiCheck :runtime:verifyCommonMainMeeseeksDatabaseMigration --stacktrace --warning-mode fail
```

Expected outcomes:

- `preflight` passes with a valid Android SDK path and required SDK components.
- `clean build` passes across configured targets in CI/release environment.
- `:runtime:jvmApiCheck` and `:runtime:klibApiCheck` both execute and pass.
- `:runtime:verifyCommonMainMeeseeksDatabaseMigration` passes.

## Preflight Caveats

- `preflight` validates SDK component presence (`platform-tools`, `licenses`, and either `build-tools` or `platforms`) and fails if the SDK path is incomplete.
- `CHROME_BIN` is optional in preflight. If browser auto-discovery fails at runtime, set `CHROME_BIN` explicitly before running `:runtime:jsTest`.

## Dependency Integrity (contributors)

Meeseeks uses Gradle dependency locking and verification metadata for reproducibility.

- Lockfiles are committed and should be updated intentionally when dependencies change.
- Verification metadata is committed to protect against unexpected artifact changes.

Typical update flow:

```bash
./gradlew :runtime:dependencies :sample:dependencies :tooling:plugins:dependencies --write-locks
./gradlew :commonizeNativeDistribution --write-locks
./gradlew --write-verification-metadata sha256 help
```

## Troubleshooting (quick)

- `SDK location not found`:
  - Set `ANDROID_HOME`/`ANDROID_SDK_ROOT` or add `sdk.dir` to `local.properties`.
- `Cannot start ChromeHeadless` or `Please set env variable CHROME_BIN`:
  - Export `CHROME_BIN` to a valid Chrome/Chromium binary path.
- `Cannot schedule task on <target>: unsupported preconditions [...]`:
  - See `docs/capabilities.md` for target support and fail-fast rules.

## Compatibility Promise

`1.x` follows SemVer for the stable API packages listed above:

- Source and binary compatibility are enforced for declared public APIs using JVM and KLib API checks.
- Behavior is platform-aware; platform-specific capability constraints are documented in `docs/capabilities.md`.
- Breaking API changes are deferred to `2.0.0`, except for security-critical fixes.

## License

Apache 2.0. See `LICENSE`.
