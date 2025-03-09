package dev.mattramotar.meeseeks.runtime.impl

import kotlin.js.Promise

internal external interface ServiceWorkerContainer {
    val ready: Promise<ServiceWorkerRegistration>
}