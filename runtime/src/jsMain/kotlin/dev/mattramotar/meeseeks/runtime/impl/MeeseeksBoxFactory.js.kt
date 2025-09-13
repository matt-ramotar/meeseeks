package dev.mattramotar.meeseeks.runtime.impl

import dev.mattramotar.meeseeks.runtime.BackgroundTaskManager
import dev.mattramotar.meeseeks.runtime.BackgroundTaskConfig
import dev.mattramotar.meeseeks.runtime.AppContext
import dev.mattramotar.meeseeks.runtime.MeeseeksRegistry

internal actual class MeeseeksBoxFactory actual constructor() {
    actual fun create(
        context: AppContext,
        registry: MeeseeksRegistry,
        config: BackgroundTaskConfig
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