package dev.mattramotar.meeseeks.runtime.internal

import dev.mattramotar.meeseeks.runtime.BGTaskManager
import dev.mattramotar.meeseeks.runtime.BGTaskManagerConfig
import dev.mattramotar.meeseeks.runtime.AppContext
import dev.mattramotar.meeseeks.runtime.internal.concurrency.synchronized
import kotlinx.serialization.json.Json
import kotlin.concurrent.Volatile

@PublishedApi
internal object BGTaskManagerSingleton {

    @Volatile
    private var _bgTaskManager: BGTaskManager? = null

    val instance: BGTaskManager
        get() {
            return _bgTaskManager ?: throw IllegalStateException("BGTaskManager is not set.")
        }

    fun getOrCreate(
        context: AppContext,
        registry: WorkerRegistry,
        json: Json,
        config: BGTaskManagerConfig = BGTaskManagerConfig(),
    ): BGTaskManager {
        val existingBGTaskManager1 = _bgTaskManager
        if (existingBGTaskManager1 != null) return existingBGTaskManager1

        return synchronized(this) {
            val existingBGTaskManager2 = _bgTaskManager
            if (existingBGTaskManager2 != null) {
                existingBGTaskManager2
            } else {
                val bgTaskManager = createBGTaskManager(context, registry, json, config)
                _bgTaskManager = bgTaskManager
                bgTaskManager
            }
        }
    }


    fun resetState() {
        synchronized(this) {
            _bgTaskManager = null
        }
    }
}

