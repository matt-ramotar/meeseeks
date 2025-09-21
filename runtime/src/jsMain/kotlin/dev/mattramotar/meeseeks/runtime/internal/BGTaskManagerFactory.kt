package dev.mattramotar.meeseeks.runtime.internal

import dev.mattramotar.meeseeks.runtime.AppContext
import dev.mattramotar.meeseeks.runtime.BGTaskManager
import dev.mattramotar.meeseeks.runtime.BGTaskManagerConfig
import dev.mattramotar.meeseeks.runtime.db.MeeseeksDatabase
import kotlinx.serialization.json.Json

internal actual class BGTaskManagerFactory actual constructor() {
    actual fun create(
        context: AppContext,
        database: MeeseeksDatabase,
        registry: WorkerRegistry,
        json: Json,
        config: BGTaskManagerConfig
    ): BGTaskManager {
        val workRequestFactory = WorkRequestFactory()
        val taskScheduler = TaskScheduler()

        BGTaskRunner.initialize(database, registry, config, taskScheduler, context)

        val taskRescheduler = TaskRescheduler(database, taskScheduler, workRequestFactory, config, registry)
        return RealBGTaskManager(
            database = database,
            workRequestFactory = workRequestFactory,
            taskScheduler = taskScheduler,
            taskRescheduler = taskRescheduler,
            registry = registry,
            config = config,
            appContext = context
        )
    }
}