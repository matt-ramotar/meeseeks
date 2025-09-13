package dev.mattramotar.meeseeks.runtime.impl

import dev.mattramotar.meeseeks.runtime.BackgroundTaskManager
import dev.mattramotar.meeseeks.runtime.BackgroundTaskConfig
import dev.mattramotar.meeseeks.runtime.AppContext
import dev.mattramotar.meeseeks.runtime.TaskWorkerRegistry
import platform.BackgroundTasks.BGTaskScheduler

internal actual class BackgroundTaskManagerFactory {
    actual fun create(
        context: AppContext,
        registry: TaskWorkerRegistry,
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