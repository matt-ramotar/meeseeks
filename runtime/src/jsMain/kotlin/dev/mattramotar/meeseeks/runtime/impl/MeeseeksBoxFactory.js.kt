package dev.mattramotar.meeseeks.runtime.impl

import dev.mattramotar.meeseeks.runtime.BackgroundTaskManager
import dev.mattramotar.meeseeks.runtime.MeeseeksBoxConfig
import dev.mattramotar.meeseeks.runtime.MeeseeksContext
import dev.mattramotar.meeseeks.runtime.MeeseeksRegistry

internal actual class MeeseeksBoxFactory actual constructor() {
    actual fun create(
        context: MeeseeksContext,
        registry: MeeseeksRegistry,
        config: MeeseeksBoxConfig
    ): BackgroundTaskManager {
        val database = MeeseeksAppDatabase.require(context)
        val workRequestFactory = WorkRequestFactory()
        val taskScheduler = TaskScheduler()
        val taskRescheduler = TaskRescheduler(database, taskScheduler, workRequestFactory)
        return RealBackgroundTaskManager(
            database = database,
            workRequestFactory = workRequestFactory,
            taskScheduler = taskScheduler,
            taskRescheduler = taskRescheduler
        )
    }
}