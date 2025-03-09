package dev.mattramotar.meeseeks.runtime.impl

internal external interface ServiceWorkerRegistration {
    val sync: SyncManager?
    val periodicSync: PeriodicSyncManager?
}