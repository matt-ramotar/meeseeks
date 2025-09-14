package dev.mattramotar.meeseeks.runtime.internal

import dev.mattramotar.meeseeks.runtime.BGTaskManager
import dev.mattramotar.meeseeks.runtime.BGTaskManagerConfig
import dev.mattramotar.meeseeks.runtime.AppContext
import platform.BackgroundTasks.BGTaskScheduler

internal actual class BGTaskManagerFactory {
    actual fun create(
        context: AppContext,
        registry: WorkerRegistry,
        config: BGTaskManagerConfig
    ): BGTaskManager {
        val database = MeeseeksAppDatabase.require(context)
        val bgTaskScheduler = BGTaskScheduler.sharedScheduler
        val workRequestFactory = WorkRequestFactory()
        val taskScheduler = TaskScheduler(database, bgTaskScheduler)
        val taskRescheduler = TaskRescheduler(database, taskScheduler, workRequestFactory)
        return RealBGTaskManager(
            database,
            workRequestFactory,
            taskScheduler,
            taskRescheduler
        )
    }
}