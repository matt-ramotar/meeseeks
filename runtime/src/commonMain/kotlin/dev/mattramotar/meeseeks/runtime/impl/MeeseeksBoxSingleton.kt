package dev.mattramotar.meeseeks.runtime.impl

import dev.mattramotar.meeseeks.runtime.BackgroundTaskManager
import dev.mattramotar.meeseeks.runtime.BackgroundTaskConfig
import dev.mattramotar.meeseeks.runtime.AppContext
import dev.mattramotar.meeseeks.runtime.MeeseeksRegistry
import dev.mattramotar.meeseeks.runtime.dsl.box.MeeseeksBox
import dev.mattramotar.meeseeks.runtime.impl.concurrency.synchronized
import kotlin.concurrent.Volatile

internal object MeeseeksBoxSingleton {

    @Volatile
    private var _backgroundTaskManager: BackgroundTaskManager? = null

    val backgroundTaskManager: BackgroundTaskManager
        get() {
            return _backgroundTaskManager ?: throw IllegalStateException("MeeseeksBox not set.")
        }

    fun getOrCreateMeeseeksBox(
        context: AppContext,
        config: BackgroundTaskConfig = BackgroundTaskConfig(),
        registry: MeeseeksRegistry
    ): BackgroundTaskManager {
        val existingBoxCheck1 = _backgroundTaskManager
        if (existingBoxCheck1 != null) return existingBoxCheck1

        return synchronized(this) {
            val existingBoxCheck2 = _backgroundTaskManager
            if (existingBoxCheck2 != null) {
                existingBoxCheck2
            } else {
                val newBox = MeeseeksBox(context, config, registry)
                _backgroundTaskManager = newBox
                newBox
            }
        }
    }


    fun resetState() {
        synchronized(this) {
            _backgroundTaskManager = null
        }
    }
}

