package dev.mattramotar.meeseeks.runtime.internal.coroutines

import kotlinx.coroutines.CoroutineDispatcher

expect object MeeseeksDispatchers {
    val IO : CoroutineDispatcher
}