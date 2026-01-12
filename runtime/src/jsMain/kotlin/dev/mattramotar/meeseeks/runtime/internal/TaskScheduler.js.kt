@file:Suppress("TooManyFunctions", "UnsafeCastFromDynamic")

package dev.mattramotar.meeseeks.runtime.internal

import dev.mattramotar.meeseeks.runtime.TaskPreconditions
import dev.mattramotar.meeseeks.runtime.TaskRequest
import dev.mattramotar.meeseeks.runtime.TaskSchedule

@JsName("setTimeout")
private external fun jsSetTimeout(handler: dynamic, timeout: Int): Int

@JsName("clearTimeout")
private external fun jsClearTimeout(handle: Int)

internal actual class TaskScheduler {

    private val scheduledTasks = mutableMapOf<String, String>()
    private val fallbackTimers = mutableMapOf<String, Int>()

    actual fun scheduleTask(
        taskId: String,
        task: TaskRequest,
        workRequest: WorkRequest,
        existingWorkPolicy: ExistingWorkPolicy
    ) {
        val uniqueName = WorkRequestFactory.uniqueWorkNameFor(taskId, task.schedule)
        val alreadyScheduled = scheduledTasks.containsKey(taskId)

        if (alreadyScheduled) {
            if (existingWorkPolicy == ExistingWorkPolicy.KEEP) return
            doCancel(taskId) // Cancel before rescheduling (REPLACE)
        }

        // Determine the initial delay for the very first activation
        val initialDelayMs = when (task.schedule) {
            is TaskSchedule.OneTime -> task.schedule.initialDelay.inWholeMilliseconds
            is TaskSchedule.Periodic -> task.schedule.initialDelay.inWholeMilliseconds
        }

        // Schedule only the first activation
        scheduleActivation(taskId, task.preconditions, initialDelayMs)
        scheduledTasks[taskId] = uniqueName
    }

    internal fun scheduleActivation(taskId: String, preconditions: TaskPreconditions, delayMs: Long) {
        clearFallbackTimer(taskId)
        val tagForRunner = WorkRequestFactory.createTag(taskId)

        if (delayMs <= 0L && preconditions.requiresNetwork) {
            // Use SyncManager if immediate execution and network are required
            try {
                scheduleWithSyncManager(tagForRunner, taskId)
            } catch (e: Throwable) {
                console.warn("SyncManager unavailable for task $taskId: ${e.message}. Falling back to setTimeout(0).")
                // Fallback to immediate timeout if SyncManager fails to register
                scheduleWithTimeout(tagForRunner, 0L, taskId)
            }
        } else {
            // Use setTimeout if immediate execution or network is not required
            scheduleWithTimeout(tagForRunner, delayMs, taskId)
        }
    }

    private fun scheduleWithSyncManager(tag: String, taskId: String) {
        val container = navigatorServiceWorker()
            ?: throw IllegalStateException("ServiceWorker not available")

        container.ready.then { registration: ServiceWorkerRegistration ->
            val syncManager = registration.sync
                ?: throw IllegalStateException("SyncManager not supported")

            syncManager.register(tag).then { }.catch { err: dynamic ->
                console.warn("SyncManager registration failed: ${err?.message}. Falling back to timeout.")
                scheduleWithTimeout(tag, 0L, taskId)
            }
        }.catch { err: dynamic ->
            console.warn("ServiceWorker ready failed: ${err?.message}. Falling back to timeout.")
            scheduleWithTimeout(tag, 0L, taskId)
        }
    }

    private fun scheduleWithTimeout(tag: String, delayMs: Long, taskId: String) {
        val delay = coerceTimeout(delayMs)
        val handle = jsSetTimeout({
            BGTaskRunner.run(tag)
        }, delay)
        fallbackTimers[taskId] = handle
    }

    private fun coerceTimeout(ms: Long): Int {
        return ms.coerceIn(0, Int.MAX_VALUE.toLong()).toInt()
    }

    actual fun isScheduled(taskId: String, taskSchedule: TaskSchedule): Boolean {
        return scheduledTasks.containsKey(taskId)
    }

    actual fun cancelWorkById(schedulerId: String, taskSchedule: TaskSchedule) {
        val entry = scheduledTasks.entries.find { it.value == schedulerId } ?: return
        doCancel(entry.key)
    }

    actual fun cancelUniqueWork(uniqueWorkName: String, taskSchedule: TaskSchedule) {
        val entry = scheduledTasks.entries.find { it.value == uniqueWorkName } ?: return
        doCancel(entry.key)
    }

    actual fun cancelAllWorkByTag(tag: String) {
        val matchingIds = scheduledTasks.filterValues { it.contains(tag) }.keys
        matchingIds.forEach { doCancel(it) }
    }

    private fun doCancel(taskId: String) {
        scheduledTasks.remove(taskId)
        clearFallbackTimer(taskId)

        try {
            removeServiceWorkerSync(taskId)
        } catch (e: Throwable) {
            console.warn("Failed to remove ServiceWorker sync/periodicSync for $taskId: ${e.message}")
        }
    }

    private fun removeServiceWorkerSync(taskId: String) {
        val container = navigatorServiceWorker() ?: return
        container.ready.then { registration ->
            val tag = WorkRequestFactory.createTag(taskId)

            registration.periodicSync?.let { periodicSyncManager ->
                periodicSyncManager.getTags().then { tags ->
                    if (tags.contains(tag)) {
                        periodicSyncManager.unregister(tag).catch { err: dynamic ->
                            console.warn("Failed to unregister periodicSync($tag): $err")
                        }
                    }
                }
            }

            registration.sync?.let { syncManager ->
                syncManager.getTags().then { tags ->
                    if (tags.contains(tag)) {
                        // Most browsers don't support unregistering for one-off Sync yet
                        console.log("Unregistering a task is not currently supported")
                    }
                }
            }
        }
    }

    internal fun clearFallbackTimer(taskId: String) {
        fallbackTimers.remove(taskId)?.let { handle ->
            jsClearTimeout(handle)
        }
    }

    internal fun removeScheduledTask(taskId: String) {
        scheduledTasks.remove(taskId)
    }

    private fun navigatorServiceWorker(): ServiceWorkerContainer? {
        return try {
            val serviceWorker = js("navigator.serviceWorker") ?: return null
            serviceWorker.unsafeCast<ServiceWorkerContainer>()
        } catch (_: Throwable) {
            null
        }
    }
}
