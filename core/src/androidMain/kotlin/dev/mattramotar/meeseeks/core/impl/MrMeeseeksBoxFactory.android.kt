package dev.mattramotar.meeseeks.core.impl

import android.util.Log
import androidx.work.Configuration
import androidx.work.WorkManager
import dev.mattramotar.meeseeks.core.MeeseeksContext
import dev.mattramotar.meeseeks.core.MeeseeksRegistry
import dev.mattramotar.meeseeks.core.MrMeeseeksBox
import dev.mattramotar.meeseeks.core.MrMeeseeksBoxConfig

internal actual class MrMeeseeksBoxFactory {
    actual fun create(
        context: MeeseeksContext,
        registry: MeeseeksRegistry,
        config: MrMeeseeksBoxConfig
    ): MrMeeseeksBox {
        val database = MeeseeksAppDatabase.require(context)
        val workerFactory = MeeseeksWorkerFactory(database, registry)
        val workRequestFactory = WorkRequestFactory(config.backoffMinimumMillis)

        val configuration = Configuration.Builder()
            .setMinimumLoggingLevel(Log.INFO)
            .setWorkerFactory(workerFactory)
            .build()

        WorkManager.Companion.initialize(context.applicationContext, configuration)
        val workManager = WorkManager.Companion.getInstance(context.applicationContext)

        val taskScheduler = TaskScheduler(workManager)
        val taskRescheduler = TaskRescheduler(database, taskScheduler, workRequestFactory)

        return RealMrMeeseeksBox(
            database,
            workRequestFactory,
            taskScheduler,
            taskRescheduler
        )
    }
}