package dev.mattramotar.meeseeks.runtime.impl

import dev.mattramotar.meeseeks.runtime.MeeseeksBox
import dev.mattramotar.meeseeks.runtime.MeeseeksBoxConfig
import dev.mattramotar.meeseeks.runtime.MeeseeksContext
import dev.mattramotar.meeseeks.runtime.MeeseeksRegistry
import platform.BackgroundTasks.BGTaskScheduler

internal actual class MeeseeksBoxFactory {
    actual fun create(
        context: MeeseeksContext,
        registry: MeeseeksRegistry,
        config: MeeseeksBoxConfig
    ): MeeseeksBox {
        val database = MeeseeksAppDatabase.require(context)
        val bgTaskScheduler = BGTaskScheduler.sharedScheduler
        val workRequestFactory = WorkRequestFactory()
        val taskScheduler = TaskScheduler(database, bgTaskScheduler)
        val taskRescheduler = TaskRescheduler(database, taskScheduler, workRequestFactory)
        return RealMeeseeksBox(
            database,
            workRequestFactory,
            taskScheduler,
            taskRescheduler
        )
    }
}