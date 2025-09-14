package dev.mattramotar.meeseeks.runtime.impl

import dev.mattramotar.meeseeks.runtime.AppContext
import dev.mattramotar.meeseeks.runtime.BGTaskManager
import dev.mattramotar.meeseeks.runtime.BackgroundTaskConfig
import dev.mattramotar.meeseeks.runtime.impl.WorkerRegistry
import org.quartz.impl.StdSchedulerFactory

internal actual class BackgroundTaskManagerFactory {
    actual fun create(
        context: AppContext,
        registry: WorkerRegistry,
        config: BackgroundTaskConfig
    ): BGTaskManager {
        val database = MeeseeksAppDatabase.require(context)
        val scheduler = StdSchedulerFactory("quartz.properties").scheduler
        scheduler.context["meeseeksDatabase"] = database
        scheduler.context["workerRegistry"] = registry
        scheduler.start()

        val workRequestFactory = WorkRequestFactory()
        val taskScheduler = TaskScheduler(scheduler)
        val taskRescheduler = TaskRescheduler(database, taskScheduler, workRequestFactory)

        return RealBackgroundTaskManager(
            database = database,
            workRequestFactory = workRequestFactory,
            taskScheduler = taskScheduler,
            taskRescheduler = taskRescheduler
        )
    }
}