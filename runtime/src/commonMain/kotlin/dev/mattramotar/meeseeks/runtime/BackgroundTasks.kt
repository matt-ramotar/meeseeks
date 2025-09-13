package dev.mattramotar.meeseeks.runtime

import dev.mattramotar.meeseeks.runtime.impl.MeeseeksAppDatabase
import dev.mattramotar.meeseeks.runtime.impl.MeeseeksBoxSingleton

object BackgroundTasks {

    fun initialize(
        context: MeeseeksContext,
        config: MeeseeksBoxConfig = MeeseeksBoxConfig(),
        registryBuilder: MeeseeksRegistry.Builder.() -> Unit
    ) {
        val registry = MeeseeksRegistry.Builder().apply(registryBuilder).build()
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
    context: MeeseeksContext,
    config: MeeseeksBoxConfig = MeeseeksBoxConfig(),
    backgroundTaskManager: BackgroundTaskManager,
    registry: MeeseeksRegistry
)