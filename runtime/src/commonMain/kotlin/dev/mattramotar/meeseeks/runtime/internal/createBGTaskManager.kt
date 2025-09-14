package dev.mattramotar.meeseeks.runtime.internal


import dev.mattramotar.meeseeks.runtime.AppContext
import dev.mattramotar.meeseeks.runtime.BGTaskManager
import dev.mattramotar.meeseeks.runtime.BGTaskManagerConfig


internal fun createBGTaskManager(
    context: AppContext,
    registry: WorkerRegistry,
    config: BGTaskManagerConfig = BGTaskManagerConfig(),
): BGTaskManager {

    val factory = BGTaskManagerFactory()

    return factory.create(
        context,
        registry,
        config
    )
}



