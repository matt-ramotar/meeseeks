# Completion Replay

Meeseeks task status observation is live. `observeStatus(taskId)` reports updates while the collector is active, but it does not buffer transitions that happen while the app process, UI, or subscription is gone.

Terminal event replay is durable. `replayTerminalEvents(sinceEventId)` and `getTaskEvents(taskId)` read persisted task log rows, so startup code can recover completions that happened while no UI observer was active.

## Startup replay pattern

Store the highest event id that your app has already handled. On startup, initialize Meeseeks, replay newer terminal events, update UI or local app state, then persist the newest event id.

```kotlin
class CompletionReplayStore {
    fun lastHandledEventId(): Long = TODO("Read from app settings or local storage")
    fun saveLastHandledEventId(id: Long) {
        TODO("Write to app settings or local storage")
    }
}

fun replayMissedCompletions(
    manager: BGTaskManager,
    store: CompletionReplayStore,
    render: (TaskEvent) -> Unit,
) {
    val events = manager.replayTerminalEvents(store.lastHandledEventId())
    events.forEach { event ->
        render(event)
    }
    events.lastOrNull()?.let { event ->
        store.saveLastHandledEventId(event.id)
    }
}
```

Use task-scoped replay when a screen knows the task it cares about:

```kotlin
fun recoverTaskScreen(
    manager: BGTaskManager,
    taskId: TaskId,
    render: (TaskEvent) -> Unit,
) {
    manager.getTaskEvents(taskId).forEach(render)
}
```

## Outcome handling

Replay events use `TaskEventOutcome`:

- `Success`: a one-time task completed successfully.
- `Failure`: a task reached permanent failure, including retry exhaustion.
- `Cancelled`: the task was explicitly cancelled.

Periodic successful runs are not terminal. They schedule the next activation and are not returned by terminal replay. A periodic task is replayed only when it reaches `Failure` or `Cancelled`.

## Live observation versus durable replay

Use live status APIs for active UI:

```kotlin
val stream = manager.observeStatus(taskId)
```

Use durable replay for startup recovery:

```kotlin
val missedEvents = manager.replayTerminalEvents(lastHandledEventId)
```

`getTaskStatus(taskId)` returns the current persisted status. Event replay returns ordered terminal events with ids that can be used as a durable cursor.

## Platform caveats

- Android wakeups are delegated to WorkManager. Replay recovers the outcome after the app next initializes Meeseeks.
- JVM and JS can replay outcomes from Meeseeks persistence after the manager is recreated.
- iOS background task wakeups are OS-managed and best effort. Replay does not guarantee an immediate background wakeup; it lets the app recover missed terminal outcomes when the app next starts or receives a background execution window.

## Verification steps

1. Schedule a one-time task and store the current replay cursor.
2. Stop collecting `observeStatus(taskId)` or close the UI that owns the collector.
3. Let the worker finish with `Success`, `Failure`, or `Cancelled`.
4. Recreate the app manager or restart the app.
5. Call `replayTerminalEvents(lastHandledEventId)` or `getTaskEvents(taskId)`.
6. Confirm the recovered event outcome matches the terminal task state and persist the newest event id.
