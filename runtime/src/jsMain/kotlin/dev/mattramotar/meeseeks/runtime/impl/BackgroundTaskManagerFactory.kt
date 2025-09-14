package dev.mattramotar.meeseeks.runtime.impl

import dev.mattramotar.meeseeks.runtime.BGTaskManager
import dev.mattramotar.meeseeks.runtime.BackgroundTaskConfig
import dev.mattramotar.meeseeks.runtime.AppContext
import dev.mattramotar.meeseeks.runtime.impl.WorkerRegistry

internal actual class BackgroundTaskManagerFactory actual constructor() {
    actual fun create(
        context: AppContext,
        registry: WorkerRegistry,
        config: BackgroundTaskConfig
    ): BGTaskManager {
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