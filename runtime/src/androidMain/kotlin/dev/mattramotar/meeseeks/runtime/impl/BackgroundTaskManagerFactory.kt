package dev.mattramotar.meeseeks.runtime.impl

import android.util.Log
import androidx.work.Configuration
import androidx.work.WorkManager
import dev.mattramotar.meeseeks.runtime.AppContext
import dev.mattramotar.meeseeks.runtime.TaskWorkerRegistry
import dev.mattramotar.meeseeks.runtime.BackgroundTaskManager
import dev.mattramotar.meeseeks.runtime.BackgroundTaskConfig

internal actual class BackgroundTaskManagerFactory {
    actual fun create(
        context: AppContext,
        registry: TaskWorkerRegistry,
        config: BackgroundTaskConfig
    ): BackgroundTaskManager {
        val database = MeeseeksAppDatabase.require(context)
        val workerFactory = BackgroundTaskWorkerFactory(database, registry)
        val workRequestFactory = WorkRequestFactory(config.backoffMinimumMillis)

        val configuration = Configuration.Builder()
            .setMinimumLoggingLevel(Log.INFO)
            .setWorkerFactory(workerFactory)
            .build()

        WorkManager.Companion.initialize(context.applicationContext, configuration)
        val workManager = WorkManager.Companion.getInstance(context.applicationContext)

        val taskScheduler = TaskScheduler(workManager)
        val taskRescheduler = TaskRescheduler(database, taskScheduler, workRequestFactory)

        return RealBackgroundTaskManager(
            database,
            workRequestFactory,
            taskScheduler,
            taskRescheduler
        )
    }
}