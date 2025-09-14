package dev.mattramotar.meeseeks.runtime.dsl.box


import dev.mattramotar.meeseeks.runtime.AppContext
import dev.mattramotar.meeseeks.runtime.BGTaskManager
import dev.mattramotar.meeseeks.runtime.BGTaskManagerConfig
import dev.mattramotar.meeseeks.runtime.impl.WorkerRegistry
import dev.mattramotar.meeseeks.runtime.impl.BackgroundTaskManagerFactory


internal fun BackgroundTaskManager(
    context: AppContext,
    registry: WorkerRegistry,
    config: BGTaskManagerConfig = BGTaskManagerConfig(),
): BGTaskManager {

    val factory = BackgroundTaskManagerFactory()

    return factory.create(
        context,
        registry,
        config
    )
}



