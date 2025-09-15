package dev.mattramotar.meeseeks.runtime.internal

import dev.mattramotar.meeseeks.runtime.AppContext
import dev.mattramotar.meeseeks.runtime.BGTaskManager
import dev.mattramotar.meeseeks.runtime.BGTaskManagerConfig
import kotlinx.serialization.json.Json

internal actual class BGTaskManagerFactory actual constructor() {
    actual fun create(
        context: AppContext,
        registry: WorkerRegistry,
        json: Json,
        config: BGTaskManagerConfig
    ): BGTaskManager {
        val database = MeeseeksAppDatabase.require(context, json)
        val workRequestFactory = WorkRequestFactory()
        val taskScheduler = TaskScheduler()
        val taskRescheduler = TaskRescheduler(database, taskScheduler, workRequestFactory, config)
        return RealBGTaskManager(
            database = database,
            workRequestFactory = workRequestFactory,
            taskScheduler = taskScheduler,
            taskRescheduler = taskRescheduler,
            config = config
        )
    }
}