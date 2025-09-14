package dev.mattramotar.meeseeks.runtime.internal

import kotlin.js.Promise

internal external interface ServiceWorkerContainer {
    val ready: Promise<ServiceWorkerRegistration>
}