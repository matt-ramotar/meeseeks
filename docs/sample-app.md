# Sample App (4-Target Demo)

This repository includes a full visual sample app that exercises the shared Meeseeks demo engine across:

- Android: `:sample:androidApp`
- Desktop: `:sample:desktopApp`
- Web Browser: `:sample:webApp`
- iOS SwiftUI host shell: `sample/iosApp` (consumes framework from `:sample:multiplatform`)

The shared orchestration and scenario engine lives in `:sample:multiplatform` under `dev.mattramotar.meeseeks.sample.demo`.

## Shared Demo API

Sample-only shared API types:

- `DemoScenario`
- `DemoScheduleRequest`
- `DemoTaskFacade`
- `DemoTelemetrySnapshot`

Shared facade operations exposed to each shell:

- Schedule one-time
- Schedule periodic
- Reschedule task
- Cancel task
- Cancel all
- Reschedule pending tasks
- List tasks
- View/export telemetry snapshot

## Run Instructions

### Android (Compose)

```bash
./gradlew :sample:androidApp:installDebug
```

Launch `Meeseeks Sample` on a connected device or emulator.

### Desktop (Compose Desktop)

```bash
./gradlew :sample:desktopApp:run
```

### Web Browser (Kotlin/JS)

```bash
./gradlew :sample:webApp:jsBrowserDevelopmentRun
```

### iOS SwiftUI

Build the KMP framework for simulator:

```bash
./gradlew :sample:multiplatform:linkDebugFrameworkIosSimulatorArm64
```

Then configure your Xcode target in `sample/iosApp/` to use the generated framework at:

`sample/multiplatform/build/bin/iosSimulatorArm64/debugFramework/sample.framework`

`sample/iosApp/ContentView.swift` is pre-wired to `DemoIosBridge`.

## Demo Verification Walkthrough

Run these checks from any shell UI:

1. Schedule `SUCCESS` one-time and verify terminal `Completed`.
2. Schedule `RETRY_THEN_SUCCESS` and verify retry transition then `Completed`.
3. Schedule `PERMANENT_FAILURE` and verify terminal `Failed`.
4. Schedule periodic task and verify repeated run cycle; then cancel.
5. Trigger `Cancel All` and `Reschedule Pending`.
6. Verify telemetry panel receives schedule/start/success/fail/retry events.
7. Use telemetry export controls and verify non-empty JSON payloads.
8. Validate encryption path by initializing shell config with `encryptionEnabled = true`.
9. Verify unsupported precondition errors surface clearly:
   - Desktop/Web: network/charging/battery constraints are unsupported.
   - iOS: battery-not-low constraint is unsupported.
10. Verify platform smoke checks via the commands below.

## Smoke Checks

Non-iOS smoke:

```bash
./gradlew sampleSmoke
```

iOS smoke:

```bash
./gradlew sampleIosSmoke
```

CI executes the same smoke gates.

## Screenshot Capture Checklist

Capture and store assets under `docs/sample-app/screenshots/` with these names:

- `android-main.png`
- `desktop-main.png`
- `web-main.png`
- `ios-main.png`
- `android-telemetry-export.png`
- `desktop-telemetry-export.png`
- `web-telemetry-export.png`
- `ios-telemetry-export.png`
