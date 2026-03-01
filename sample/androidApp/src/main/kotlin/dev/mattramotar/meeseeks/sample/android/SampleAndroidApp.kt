package dev.mattramotar.meeseeks.sample.android

import android.app.Application
import androidx.work.Configuration
import androidx.work.DelegatingWorkerFactory
import dev.mattramotar.meeseeks.runtime.MeeseeksWorkerFactory
import dev.mattramotar.meeseeks.sample.demo.DemoSampleConfig
import dev.mattramotar.meeseeks.sample.demo.DemoTaskFacade
import dev.mattramotar.meeseeks.sample.demo.DemoTaskFacadeFactory

class SampleAndroidApp : Application(), Configuration.Provider {

    companion object {
        lateinit var facade: DemoTaskFacade
            private set
    }

    override fun onCreate() {
        super.onCreate()
        facade = DemoTaskFacadeFactory.create(
            appContext = applicationContext,
            config = DemoSampleConfig(encryptionEnabled = false),
        )
    }

    override val workManagerConfiguration: Configuration by lazy {
        val factory = DelegatingWorkerFactory().apply {
            addFactory(MeeseeksWorkerFactory(facade.taskManager))
        }

        Configuration.Builder()
            .setWorkerFactory(factory)
            .build()
    }
}
