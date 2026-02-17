# Meeseeks

![Meeseeks](.github/meeseeks.svg) 

Kotlin Multiplatform runtime for scheduling and managing background tasks across Android, JVM, JS, and iOS.

## Prerequisites

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

## Troubleshooting

- `SDK location not found`:
  - Set `ANDROID_HOME`/`ANDROID_SDK_ROOT` or add `sdk.dir` to `local.properties`.
- `Cannot start ChromeHeadless` or `Please set env variable CHROME_BIN`:
  - Export `CHROME_BIN` to a valid Chrome/Chromium binary path.

## License

Apache 2.0. See `LICENSE`.
