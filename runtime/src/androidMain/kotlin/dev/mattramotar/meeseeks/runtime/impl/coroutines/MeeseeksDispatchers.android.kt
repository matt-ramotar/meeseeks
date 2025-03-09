package dev.mattramotar.meeseeks.runtime.impl.coroutines

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

actual object MeeseeksDispatchers {
    actual val IO: CoroutineDispatcher = Dispatchers.IO
}