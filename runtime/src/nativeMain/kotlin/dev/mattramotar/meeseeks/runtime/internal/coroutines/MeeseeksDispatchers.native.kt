package dev.mattramotar.meeseeks.runtime.internal.coroutines

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

actual object MeeseeksDispatchers {
    actual val IO: CoroutineDispatcher = Dispatchers.IO
}