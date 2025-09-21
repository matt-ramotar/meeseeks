package dev.mattramotar.meeseeks.sample

import android.app.Application
import androidx.work.Configuration
import androidx.work.DelegatingWorkerFactory
import dev.mattramotar.meeseeks.runtime.Meeseeks
import dev.mattramotar.meeseeks.runtime.MeeseeksWorkerFactory
import dev.mattramotar.meeseeks.sample.shared.RefreshPayload
import dev.mattramotar.meeseeks.sample.shared.RefreshWorker
import dev.mattramotar.meeseeks.sample.shared.SyncPayload
import dev.mattramotar.meeseeks.sample.shared.SyncWorker
import kotlin.time.Duration.Companion.seconds

class App : Application(), Configuration.Provider {

    private val bgTaskManager by lazy {
        Meeseeks.initialize(applicationContext) {
            minBackoff(20.seconds)
            maxRetryCount(3)
            maxParallelTasks(5)
            allowExpedited()
            register<SyncPayload>(SyncPayload.stableId) { SyncWorker(applicationContext) }
            register<RefreshPayload>(RefreshPayload.stableId) { RefreshWorker(applicationContext) }
        }

    }

    override val workManagerConfiguration: Configuration by lazy {
        val delegating = DelegatingWorkerFactory().apply {
            addFactory(MeeseeksWorkerFactory(bgTaskManager))
        }
        Configuration.Builder()
            .setWorkerFactory(delegating)
            .build()
    }
}