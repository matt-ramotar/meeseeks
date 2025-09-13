package dev.mattramotar.meeseeks.runtime.dsl.box


import dev.mattramotar.meeseeks.runtime.AppContext
import dev.mattramotar.meeseeks.runtime.MeeseeksRegistry
import dev.mattramotar.meeseeks.runtime.BackgroundTaskManager
import dev.mattramotar.meeseeks.runtime.BackgroundTaskConfig
import dev.mattramotar.meeseeks.runtime.impl.MeeseeksBoxFactory


fun MeeseeksBox(
    context: AppContext,
    config: BackgroundTaskConfig = BackgroundTaskConfig(),
    registry: MeeseeksRegistry
): BackgroundTaskManager {

    val factory = MeeseeksBoxFactory()

    return factory.create(
        context,
        registry,
        config
    )
}



