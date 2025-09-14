package dev.mattramotar.meeseeks.runtime.internal

internal external interface ServiceWorkerRegistration {
    val sync: SyncManager?
    val periodicSync: PeriodicSyncManager?
}