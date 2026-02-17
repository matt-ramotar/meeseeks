# iOS Integration

## Requirements

- iOS BackgroundTasks enabled in app configuration
- `BGTaskSchedulerPermittedIdentifiers` contains Meeseeks identifiers

Identifiers used by Meeseeks:

- `dev.mattramotar.meeseeks.task.refresh`
- `dev.mattramotar.meeseeks.task.processing`

These values are defined in `runtime/src/nativeMain/kotlin/dev/mattramotar/meeseeks/runtime/BGTaskIdentifiers.kt`.

## Info.plist configuration

Add required identifiers:

```xml
<key>BGTaskSchedulerPermittedIdentifiers</key>
<array>
    <string>dev.mattramotar.meeseeks.task.refresh</string>
    <string>dev.mattramotar.meeseeks.task.processing</string>
</array>
```

Ensure background modes are aligned with your usage (for example, app refresh and processing modes).

## Constraint behavior on iOS

- Supported: `requiresNetwork`, `requiresCharging`
- Unsupported (fail-fast): `requiresBatteryNotLow`

See `docs/capabilities.md` for full matrix and fail-fast semantics.

## Scheduling caveats

- iOS wakeups are OS-managed and best-effort.
- Meeseeks database is the source of truth; platform task requests are hints to the OS.
- At the end of execution windows, Meeseeks may submit follow-up refresh/processing requests based on pending work.
