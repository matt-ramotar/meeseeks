@file:Suppress("TooManyFunctions", "UnsafeCastFromDynamic")

package dev.mattramotar.meeseeks.runtime.internal

import dev.mattramotar.meeseeks.runtime.TaskRequest
import dev.mattramotar.meeseeks.runtime.TaskSchedule

@JsName("setTimeout")
private external fun jsSetTimeout(handler: dynamic, timeout: Int): Int

@JsName("clearTimeout")
private external fun jsClearTimeout(handle: Int)

@JsName("setInterval")
private external fun jsSetInterval(handler: dynamic, interval: Int): Int

@JsName("clearInterval")
private external fun jsClearInterval(handle: Int)

internal actual class TaskScheduler {

    private val scheduledTasks = mutableMapOf<Long, String>()
    private val fallbackTimers = mutableMapOf<Long, Int>()

    actual fun scheduleTask(
        taskId: Long,
        task: TaskRequest,
        workRequest: WorkRequest,
        existingWorkPolicy: ExistingWorkPolicy
    ) {
        val uniqueName = WorkRequestFactory.uniqueWorkNameFor(taskId, task.schedule)
        val tagForRunner = WorkRequestFactory.createTag(taskId)
        val alreadyScheduled = scheduledTasks.containsKey(taskId)

        when (existingWorkPolicy) {
            ExistingWorkPolicy.KEEP -> {
                if (alreadyScheduled) return
            }

            ExistingWorkPolicy.REPLACE -> {
                if (alreadyScheduled) {
                    cancelUniqueWork(uniqueName, task.schedule)
                }
            }
        }

        try {
            scheduleWithServiceWorker(task, taskId, tagForRunner)
        } catch (e: Throwable) {
            console.warn("scheduleWithServiceWorker failed: ${e.message}, using fallback")
            fallbackSchedule(task, taskId)
        }

        scheduledTasks[taskId] = uniqueName
    }


    private fun scheduleWithServiceWorker(
        task: TaskRequest,
        taskId: Long,
        tagForRunner: String
    ) {
        val container = navigatorServiceWorker()
            ?: throw IllegalStateException("ServiceWorker not available")

        val readyPromise = container.ready
        readyPromise.then { registration: ServiceWorkerRegistration ->
            when (val schedule = task.schedule) {
                is TaskSchedule.OneTime -> {
                    val syncManager = registration.sync
                        ?: throw IllegalStateException("SyncManager not supported")
                    syncManager.register(tagForRunner).catch { err: dynamic ->
                        console.warn("SyncManager registration failed: ${err?.message}")
                        fallbackSchedule(task, taskId)
                    }
                }

                is TaskSchedule.Periodic -> {
                    val periodicSync = registration.periodicSync
                        ?: throw IllegalStateException("PeriodicSyncManager not supported")
                    val intervalMs = schedule.interval.inWholeMilliseconds

                    val options: dynamic = {}
                    options.minInterval = intervalMs
                    periodicSync.register(tagForRunner, options).catch { err: dynamic ->
                        console.warn("PeriodicSync registration failed: ${err?.message}")
                        fallbackSchedule(task, taskId)
                    }
                }
            }
        }.catch { err: dynamic ->
            console.warn("ServiceWorker registration.ready promise failed: ${err?.message}")
            fallbackSchedule(task, taskId)
        }
    }

    private fun fallbackSchedule(task: TaskRequest, taskId: Long) {
        clearFallbackTimer(taskId)
        val tag = WorkRequestFactory.createTag(taskId)

        when (val schedule = task.schedule) {
            is TaskSchedule.OneTime -> {
                val delay = coerceTimeout(schedule.initialDelay.inWholeMilliseconds)
                val handle = jsSetTimeout({
                    BGTaskRunner.run(tag)
                }, delay)
                fallbackTimers[taskId] = handle
            }

            is TaskSchedule.Periodic -> {
                val interval = coerceTimeout(schedule.interval.inWholeMilliseconds)
                val handle = jsSetInterval({
                    BGTaskRunner.run(tag)
                }, interval)
                fallbackTimers[taskId] = handle
            }
        }
    }

    private fun coerceTimeout(ms: Long): Int {
        return ms.coerceIn(0, Int.MAX_VALUE.toLong()).toInt()
    }

    actual fun isScheduled(taskId: Long, taskSchedule: TaskSchedule): Boolean {
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

    private fun doCancel(taskId: Long) {
        scheduledTasks.remove(taskId)
        clearFallbackTimer(taskId)

        try {
            removeServiceWorkerSync(taskId)
        } catch (e: Throwable) {
            console.warn("Failed to remove ServiceWorker sync/periodicSync for $taskId: ${e.message}")
        }
    }

    private fun removeServiceWorkerSync(taskId: Long) {
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

    private fun clearFallbackTimer(taskId: Long) {
        fallbackTimers.remove(taskId)?.let { handle ->
            jsClearTimeout(handle)
            jsClearInterval(handle)
        }
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

