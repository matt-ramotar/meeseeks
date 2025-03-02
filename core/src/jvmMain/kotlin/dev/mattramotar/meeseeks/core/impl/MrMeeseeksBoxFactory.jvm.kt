package dev.mattramotar.meeseeks.core.impl

import dev.mattramotar.meeseeks.core.MeeseeksContext
import dev.mattramotar.meeseeks.core.MeeseeksRegistry
import dev.mattramotar.meeseeks.core.MrMeeseeksBox
import dev.mattramotar.meeseeks.core.MrMeeseeksBoxConfig

internal actual class MrMeeseeksBoxFactory {
    actual fun create(
        context: MeeseeksContext,
        registry: MeeseeksRegistry,
        config: MrMeeseeksBoxConfig
    ): MrMeeseeksBox {
        val database = MeeseeksAppDatabase.require(context)
        val workRequestFactory = WorkRequestFactory()
        val taskScheduler = TaskScheduler(database, registry)
        val taskRescheduler = TaskRescheduler(database, taskScheduler, workRequestFactory)

        return RealMrMeeseeksBox(
            database,
            workRequestFactory,
            taskScheduler,
            taskRescheduler
        )
    }
}