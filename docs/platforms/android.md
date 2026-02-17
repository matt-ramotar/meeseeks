# Android Integration

## Requirements

- AndroidX WorkManager available in your app
- Application-level `WorkManager` configuration using `MeeseeksWorkerFactory`

## Initialize in `Application`

```kotlin
class App : Application(), Configuration.Provider {
    private val bgTaskManager by lazy {
        Meeseeks.initialize(applicationContext) {
            minBackoff(20.seconds)
            maxRetryCount(3)
            maxParallelTasks(5)
            allowExpedited()

            register<SyncPayload> { SyncWorker(applicationContext) }
            register<RefreshPayload> { RefreshWorker(applicationContext) }
        }
    }

    override val workManagerConfiguration: Configuration by lazy {
        val delegating = DelegatingWorkerFactory().apply {
            addFactory(MeeseeksWorkerFactory(bgTaskManager))
        }
        Configuration.Builder()
            .setWorkerFactory(delegating)
            .build()
    }
}
```

## Common pitfalls

- `WorkManager is not initialized`:
  - ensure the default WorkManager initializer is enabled, or on-demand initialization is configured correctly
- `BGTaskCoroutineWorker` is not created:
  - ensure `MeeseeksWorkerFactory(bgTaskManager)` is added to your `DelegatingWorkerFactory`
- Unexpected retries:
  - Meeseeks owns retry policy; WorkManager internal retries are intentionally avoided by worker completion behavior

## Constraint behavior

Android supports all preconditions:

- `requiresNetwork`
- `requiresCharging`
- `requiresBatteryNotLow`

See `docs/capabilities.md` for the full cross-platform matrix.
