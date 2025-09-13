package dev.mattramotar.meeseeks.runtime

import dev.mattramotar.meeseeks.runtime.impl.MeeseeksAppDatabase
import dev.mattramotar.meeseeks.runtime.impl.MeeseeksBoxSingleton

object BackgroundTasks {

    fun initialize(
        context: AppContext,
        config: BackgroundTaskConfig = BackgroundTaskConfig(),
        registryBuilder: TaskWorkerRegistry.Builder.() -> Unit
    ) {
        val registry = TaskWorkerRegistry.Builder().apply(registryBuilder).build()
        MeeseeksAppDatabase.init(context)
        val meeseeksBox = MeeseeksBoxSingleton.getOrCreateMeeseeksBox(context, config, registry)
        initializePlatformSpecificScheduling(
            context,
            config,
            meeseeksBox,
            registry
        )
    }
}

expect fun initializePlatformSpecificScheduling(
    context: AppContext,
    config: BackgroundTaskConfig = BackgroundTaskConfig(),
    backgroundTaskManager: BackgroundTaskManager,
    registry: TaskWorkerRegistry
)