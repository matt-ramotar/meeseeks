package dev.mattramotar.meeseeks.runtime.internal

import android.util.Log
import androidx.work.Configuration
import androidx.work.WorkManager
import dev.mattramotar.meeseeks.runtime.AppContext
import dev.mattramotar.meeseeks.runtime.BGTaskManager
import dev.mattramotar.meeseeks.runtime.BGTaskManagerConfig
import kotlinx.serialization.json.Json

internal actual class BGTaskManagerFactory {
    actual fun create(
        context: AppContext,
        registry: WorkerRegistry,
        json: Json,
        config: BGTaskManagerConfig
    ): BGTaskManager {
        val database = MeeseeksAppDatabase.require(context, json)
        val workerFactory = BGTaskWorkerFactory(database, registry)
        val workRequestFactory = WorkRequestFactory(config.minBackoff.inWholeMilliseconds)

        val configuration = Configuration.Builder()
            .setMinimumLoggingLevel(Log.INFO)
            .setWorkerFactory(workerFactory)
            .build()

        WorkManager.initialize(context.applicationContext, configuration)
        val workManager = WorkManager.getInstance(context.applicationContext)

        val taskScheduler = TaskScheduler(workManager)
        val taskRescheduler = TaskRescheduler(database, taskScheduler, workRequestFactory, config)

        return RealBGTaskManager(
            database,
            workRequestFactory,
            taskScheduler,
            taskRescheduler,
            config
        )
    }
}