package dev.mattramotar.meeseeks.runtime.internal.coroutines

import kotlinx.coroutines.CoroutineDispatcher

internal expect object MeeseeksDispatchers {
    val IO: CoroutineDispatcher
}
