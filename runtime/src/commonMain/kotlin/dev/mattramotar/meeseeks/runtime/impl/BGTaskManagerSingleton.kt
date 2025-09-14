package dev.mattramotar.meeseeks.runtime.impl

import dev.mattramotar.meeseeks.runtime.BGTaskManager
import dev.mattramotar.meeseeks.runtime.BGTaskManagerConfig
import dev.mattramotar.meeseeks.runtime.AppContext
import dev.mattramotar.meeseeks.runtime.dsl.box.BGTaskManager
import dev.mattramotar.meeseeks.runtime.impl.concurrency.synchronized
import kotlin.concurrent.Volatile

internal object BGTaskManagerSingleton {

    @Volatile
    private var _manager: BGTaskManager? = null

    val instance: BGTaskManager
        get() {
            return _manager ?: throw IllegalStateException("BackgroundTaskManager is not set.")
        }

    fun getOrCreate(
        context: AppContext,
        registry: WorkerRegistry,
        config: BGTaskManagerConfig = BGTaskManagerConfig(),
    ): BGTaskManager {
        val existingManager1 = _manager
        if (existingManager1 != null) return existingManager1

        return synchronized(this) {
            val existingManager2 = _manager
            if (existingManager2 != null) {
                existingManager2
            } else {
                val newManager = BGTaskManager(context, registry, config)
                _manager = newManager
                newManager
            }
        }
    }


    fun resetState() {
        synchronized(this) {
            _manager = null
        }
    }
}

