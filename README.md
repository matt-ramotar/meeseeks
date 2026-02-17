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

## Contributor Prerequisites

- JDK 17
- Android SDK configured via one of:
  - `ANDROID_HOME`
  - `ANDROID_SDK_ROOT`
  - `local.properties` with `sdk.dir=/absolute/path/to/Android/sdk`
- Chrome/Chromium binary for JS tests:
  - `CHROME_BIN` must point to an existing browser binary

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

2. Configure browser binary for JS tests:

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

## License

Apache 2.0. See `LICENSE`.
