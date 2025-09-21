package dev.mattramotar.meeseeks.runtime.internal

import dev.mattramotar.meeseeks.runtime.AppContext
import dev.mattramotar.meeseeks.runtime.BGTaskManager
import dev.mattramotar.meeseeks.runtime.BGTaskManagerConfig
import dev.mattramotar.meeseeks.runtime.BGTaskRunner
import dev.mattramotar.meeseeks.runtime.db.MeeseeksDatabase
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.serialization.json.Json
import platform.BackgroundTasks.BGTaskScheduler

@OptIn(ExperimentalForeignApi::class)
internal actual class BGTaskManagerFactory {
    actual fun create(
        context: AppContext,
        database: MeeseeksDatabase,
        registry: WorkerRegistry,
        json: Json,
        config: BGTaskManagerConfig
    ): BGTaskManager {
        val bgTaskScheduler = BGTaskScheduler.sharedScheduler
        val nativeTaskScheduler = NativeTaskScheduler(bgTaskScheduler)
        val dependencies = MeeseeksDependencies(
            database = database,
            registry = registry,
            appContext = context,
            config = config,
        )
        val runner = BGTaskRunner(dependencies)
        val nativeCoordinator = NativeTaskCoordinator(database, nativeTaskScheduler, runner)
        val bgTaskRegistry = BGTaskRegistry(bgTaskScheduler, nativeCoordinator)

        val platformSchedulerAdapter = TaskScheduler(database, nativeTaskScheduler)

        val workRequestFactory = WorkRequestFactory()
        val taskRescheduler = TaskRescheduler(database, platformSchedulerAdapter, workRequestFactory, config, registry)

        bgTaskRegistry.registerHandlers()

        return RealBGTaskManager(
            database = database,
            workRequestFactory = workRequestFactory,
            taskScheduler = platformSchedulerAdapter,
            taskRescheduler = taskRescheduler,
            config = config,
            registry = registry,
            telemetry = config.telemetry,
            appContext = context
        )
    }
}