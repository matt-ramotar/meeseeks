# Meeseeks iOS Sample Shell

This directory contains the SwiftUI shell scaffolding for the multi-target sample app.

## Expected integration

1. Build the KMP framework from `:sample:multiplatform` for iOS:

   ```bash
   ./gradlew :sample:multiplatform:linkDebugFrameworkIosSimulatorArm64
   ```

2. Create/open an Xcode SwiftUI app target in this directory.
3. Add the generated `sample` framework to the app target from:

   `sample/multiplatform/build/bin/iosSimulatorArm64/debugFramework/sample.framework`

4. Use `DemoIosBridge` to drive the shared scenario engine from SwiftUI.

## Included files

- `SampleIOSApp.swift`: `@main` app entry
- `ContentView.swift`: parity shell UI wired to `DemoIosBridge`
