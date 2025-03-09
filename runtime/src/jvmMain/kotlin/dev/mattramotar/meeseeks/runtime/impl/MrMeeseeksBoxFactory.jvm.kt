package dev.mattramotar.meeseeks.runtime.impl

import dev.mattramotar.meeseeks.runtime.MeeseeksBox
import dev.mattramotar.meeseeks.runtime.MeeseeksBoxConfig
import dev.mattramotar.meeseeks.runtime.MeeseeksContext
import dev.mattramotar.meeseeks.runtime.MeeseeksRegistry
import org.quartz.impl.StdSchedulerFactory

internal actual class MeeseeksBoxFactory {
    actual fun create(
        context: MeeseeksContext,
        registry: MeeseeksRegistry,
        config: MeeseeksBoxConfig
    ): MeeseeksBox {
        val database = MeeseeksAppDatabase.require(context)
        val scheduler = StdSchedulerFactory("quartz.properties").scheduler
        scheduler.context["meeseeksDatabase"] = database
        scheduler.context["meeseeksRegistry"] = registry
        scheduler.start()

        val workRequestFactory = WorkRequestFactory()
        val taskScheduler = TaskScheduler(scheduler)
        val taskRescheduler = TaskRescheduler(database, taskScheduler, workRequestFactory)

        return RealMeeseeksBox(
            database = database,
            workRequestFactory = workRequestFactory,
            taskScheduler = taskScheduler,
            taskRescheduler = taskRescheduler
        )
    }
}