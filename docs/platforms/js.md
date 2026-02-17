# JS Integration

## Runtime model

Meeseeks JS scheduling uses:

- `setTimeout` fallback timers
- Service Worker SyncManager path for immediate network-triggered internal activations

The scheduler keeps runtime state and re-activates tasks from Meeseeks persistence.

## Public constraint support

JS currently supports no public preconditions.

- Unsupported (fail-fast): `requiresNetwork`, `requiresCharging`, `requiresBatteryNotLow`

If requested, `schedule(...)` / `reschedule(...)` throw `IllegalArgumentException`.
See `docs/capabilities.md`.

## Testing requirements

JS tests run in ChromeHeadless. Set `CHROME_BIN` before running:

```bash
# macOS
export CHROME_BIN="/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"
```

```bash
# Linux
export CHROME_BIN=/usr/bin/google-chrome
```

Then run:

```bash
./gradlew :runtime:jsTest --stacktrace
```

## Common pitfalls

- `Cannot start ChromeHeadless`:
  - set `CHROME_BIN` to a valid Chrome/Chromium binary
- Service worker APIs unavailable:
  - Meeseeks falls back to timer-based activations when SyncManager/service worker paths are unavailable
