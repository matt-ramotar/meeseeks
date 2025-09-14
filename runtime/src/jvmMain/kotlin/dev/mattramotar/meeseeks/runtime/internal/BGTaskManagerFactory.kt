package dev.mattramotar.meeseeks.runtime.internal

import dev.mattramotar.meeseeks.runtime.AppContext
import dev.mattramotar.meeseeks.runtime.BGTaskManager
import dev.mattramotar.meeseeks.runtime.BGTaskManagerConfig
import org.quartz.impl.StdSchedulerFactory

internal actual class BGTaskManagerFactory {
    actual fun create(
        context: AppContext,
        registry: WorkerRegistry,
        config: BGTaskManagerConfig
    ): BGTaskManager {
        val database = MeeseeksAppDatabase.require(context)
        val scheduler = StdSchedulerFactory("quartz.properties").scheduler
        scheduler.context["meeseeksDatabase"] = database
        scheduler.context["workerRegistry"] = registry
        scheduler.start()

        val workRequestFactory = WorkRequestFactory()
        val taskScheduler = TaskScheduler(scheduler)
        val taskRescheduler = TaskRescheduler(database, taskScheduler, workRequestFactory)

        return RealBGTaskManager(
            database = database,
            workRequestFactory = workRequestFactory,
            taskScheduler = taskScheduler,
            taskRescheduler = taskRescheduler
        )
    }
}