package dev.mattramotar.meeseeks.runtime.dsl.box


import dev.mattramotar.meeseeks.runtime.AppContext
import dev.mattramotar.meeseeks.runtime.BGTaskManager
import dev.mattramotar.meeseeks.runtime.BGTaskManagerConfig
import dev.mattramotar.meeseeks.runtime.impl.WorkerRegistry
import dev.mattramotar.meeseeks.runtime.impl.BGTaskManagerFactory


internal fun BGTaskManager(
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



