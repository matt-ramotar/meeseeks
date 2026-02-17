# Platform Capabilities

Meeseeks enforces target capability support at schedule time.
If you request unsupported preconditions, `schedule(...)` and `reschedule(...)` fail fast with `IllegalArgumentException`.

## Preconditions support matrix

| Target | `requiresNetwork` | `requiresCharging` | `requiresBatteryNotLow` |
|---|---|---|---|
| Android | Supported | Supported | Supported |
| iOS | Supported | Supported | Not supported |
| JVM | Not supported | Not supported | Not supported |
| JS | Not supported | Not supported | Not supported |

## What fail-fast means

When a target does not support a precondition:

- the task is not inserted/scheduled on that target
- Meeseeks throws immediately, including unsupported keys in the message
- there is no silent downgrade of task constraints

Example error shape:

```text
Cannot schedule task on JVM: unsupported preconditions [requiresNetwork].
Supported on JVM: none.
```

## Scheduling model notes

Meeseeks uses one-time platform activations and chains future runs from runtime state.
Periodic and retry behavior are still supported, but platform schedulers are driven by next activation, not platform-native periodic job primitives.
