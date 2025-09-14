package dev.mattramotar.meeseeks.runtime.dsl.box


import dev.mattramotar.meeseeks.runtime.AppContext
import dev.mattramotar.meeseeks.runtime.BGTaskManager
import dev.mattramotar.meeseeks.runtime.BackgroundTaskConfig
import dev.mattramotar.meeseeks.runtime.impl.WorkerRegistry
import dev.mattramotar.meeseeks.runtime.impl.BackgroundTaskManagerFactory


internal fun BackgroundTaskManager(
    context: AppContext,
    registry: WorkerRegistry,
    config: BackgroundTaskConfig = BackgroundTaskConfig(),
): BGTaskManager {

    val factory = BackgroundTaskManagerFactory()

    return factory.create(
        context,
        registry,
        config
    )
}



