package dev.mattramotar.meeseeks.runtime.impl

import dev.mattramotar.meeseeks.runtime.BackgroundTaskManager
import dev.mattramotar.meeseeks.runtime.BackgroundTaskConfig
import dev.mattramotar.meeseeks.runtime.AppContext
import dev.mattramotar.meeseeks.runtime.MeeseeksRegistry
import platform.BackgroundTasks.BGTaskScheduler

internal actual class MeeseeksBoxFactory {
    actual fun create(
        context: AppContext,
        registry: MeeseeksRegistry,
        config: BackgroundTaskConfig
    ): BackgroundTaskManager {
        val database = MeeseeksAppDatabase.require(context)
        val bgTaskScheduler = BGTaskScheduler.sharedScheduler
        val workRequestFactory = WorkRequestFactory()
        val taskScheduler = TaskScheduler(database, bgTaskScheduler)
        val taskRescheduler = TaskRescheduler(database, taskScheduler, workRequestFactory)
        return RealBackgroundTaskManager(
            database,
            workRequestFactory,
            taskScheduler,
            taskRescheduler
        )
    }
}