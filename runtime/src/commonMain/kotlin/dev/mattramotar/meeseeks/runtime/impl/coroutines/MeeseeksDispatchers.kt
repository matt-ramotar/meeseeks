package dev.mattramotar.meeseeks.runtime.impl.coroutines

import kotlinx.coroutines.CoroutineDispatcher

expect object MeeseeksDispatchers {
    val IO : CoroutineDispatcher
}