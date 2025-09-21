package dev.mattramotar.meeseeks.runtime.internal

import androidx.work.WorkManager
import dev.mattramotar.meeseeks.runtime.AppContext
import dev.mattramotar.meeseeks.runtime.BGTaskManager
import dev.mattramotar.meeseeks.runtime.BGTaskManagerConfig
import dev.mattramotar.meeseeks.runtime.db.MeeseeksDatabase
import kotlinx.serialization.json.Json

internal actual class BGTaskManagerFactory {
    actual fun create(
        context: AppContext,
        database: MeeseeksDatabase,
        registry: WorkerRegistry,
        json: Json,
        config: BGTaskManagerConfig
    ): BGTaskManager {
        val workRequestFactory = WorkRequestFactory(config.minBackoff.inWholeMilliseconds)

        val workManager = try {
            WorkManager.getInstance(context.applicationContext)
        } catch (e: IllegalStateException) {
            throw IllegalStateException(
                "WorkManager is not initialized. Ensure the default WorkManager initializer is enabled or On-Demand initialization is configured in your application class.",
                e
            )
        }

        val taskScheduler = TaskScheduler(workManager)
        val taskRescheduler = TaskRescheduler(database, taskScheduler, workRequestFactory, config, registry)

        return RealBGTaskManager(
            database,
            workRequestFactory,
            taskScheduler,
            taskRescheduler,
            config,
            registry = registry,
            telemetry = config.telemetry,
            appContext = context
        )
    }
}