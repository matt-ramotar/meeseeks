package dev.mattramotar.meeseeks.runtime.internal

import dev.mattramotar.meeseeks.runtime.AppContext
import dev.mattramotar.meeseeks.runtime.BGTaskManager
import dev.mattramotar.meeseeks.runtime.BGTaskManagerConfig
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.serialization.json.Json
import platform.BackgroundTasks.BGTaskScheduler

@OptIn(ExperimentalForeignApi::class)
internal actual class BGTaskManagerFactory {
    actual fun create(
        context: AppContext,
        registry: WorkerRegistry,
        json: Json,
        config: BGTaskManagerConfig
    ): BGTaskManager {
        val database = MeeseeksAppDatabase.require(context, json)
        val bgTaskScheduler = BGTaskScheduler.sharedScheduler
        val nativeTaskScheduler = NativeTaskScheduler(bgTaskScheduler)
        val nativeCoordinator = NativeTaskCoordinator(database, nativeTaskScheduler)
        val bgTaskRegistry = BGTaskRegistry(bgTaskScheduler, nativeCoordinator)

        val platformSchedulerAdapter = TaskScheduler(database, nativeTaskScheduler)

        val workRequestFactory = WorkRequestFactory()
        val taskRescheduler = TaskRescheduler(database, platformSchedulerAdapter, workRequestFactory, config)

        bgTaskRegistry.registerHandlers()

        return RealBGTaskManager(
            database = database,
            workRequestFactory = workRequestFactory,
            taskScheduler = platformSchedulerAdapter,
            taskRescheduler = taskRescheduler,
            config = config,
            telemetry = config.telemetry
        )
    }
}