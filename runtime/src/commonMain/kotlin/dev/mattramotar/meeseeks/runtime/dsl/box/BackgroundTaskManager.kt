package dev.mattramotar.meeseeks.runtime.dsl.box


import dev.mattramotar.meeseeks.runtime.AppContext
import dev.mattramotar.meeseeks.runtime.TaskWorkerRegistry
import dev.mattramotar.meeseeks.runtime.BackgroundTaskManager
import dev.mattramotar.meeseeks.runtime.BackgroundTaskConfig
import dev.mattramotar.meeseeks.runtime.impl.BackgroundTaskManagerFactory


fun BackgroundTaskManager(
    context: AppContext,
    config: BackgroundTaskConfig = BackgroundTaskConfig(),
    registry: TaskWorkerRegistry
): BackgroundTaskManager {

    val factory = BackgroundTaskManagerFactory()

    return factory.create(
        context,
        registry,
        config
    )
}



