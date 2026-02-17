# Troubleshooting

## Build setup errors

### `SDK location not found`

Set one of:

- `ANDROID_HOME`
- `ANDROID_SDK_ROOT`
- `local.properties` with `sdk.dir=/absolute/path/to/Android/sdk`

Validate with:

```bash
./gradlew preflight --stacktrace
```

### `Cannot start ChromeHeadless` / `Please set env variable CHROME_BIN`

Set:

```bash
export CHROME_BIN="/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"
```

or Linux equivalent (`/usr/bin/google-chrome`), then rerun JS tests.

## Scheduling and runtime errors

### `Cannot schedule task on <target>: unsupported preconditions [...]`

Cause:

- Task preconditions are not supported for the current target.

Fix:

- Remove unsupported flags for that target, or gate scheduling logic by platform.
- See `docs/capabilities.md`.

### `WorkManager is not initialized`

Cause:

- Android app did not initialize WorkManager correctly before Meeseeks integration.

Fix:

- Ensure default WorkManager initializer is enabled, or configure on-demand initialization.
- Ensure your `Configuration.Provider` uses `MeeseeksWorkerFactory`.

## Diagnostic commands

```bash
./gradlew preflight --stacktrace
./gradlew :runtime:jvmTest --stacktrace
./gradlew :runtime:jsTest --stacktrace
./gradlew :sample:compileKotlinJvm --stacktrace
./gradlew :sample:compileKotlinJs --stacktrace
```
