package dev.mattramotar.meeseeks.core.impl

import dev.mattramotar.meeseeks.core.MeeseeksContext
import dev.mattramotar.meeseeks.core.MeeseeksRegistry
import dev.mattramotar.meeseeks.core.MeeseeksBox
import dev.mattramotar.meeseeks.core.MeeseeksBoxConfig

internal actual class MeeseeksBoxFactory {
    actual fun create(
        context: MeeseeksContext,
        registry: MeeseeksRegistry,
        config: MeeseeksBoxConfig
    ): MeeseeksBox {
        val database = MeeseeksAppDatabase.require(context)
        val workRequestFactory = WorkRequestFactory()
        val taskScheduler = TaskScheduler(database, registry)
        val taskRescheduler = TaskRescheduler(database, taskScheduler, workRequestFactory)

        return RealMeeseeksBox(
            database,
            workRequestFactory,
            taskScheduler,
            taskRescheduler
        )
    }
}