package dev.mattramotar.meeseeks.runtime

import dev.mattramotar.meeseeks.runtime.internal.BGTaskManagerSingleton

object Meeseeks {

    fun initialize(context: AppContext, configure: ConfigurationScope.() -> Unit = {}) {
        val configurationScope = ConfigurationScope(context)
        configurationScope.configure()
        configurationScope.build()
    }

    fun bgTaskManager(): BGTaskManager {
        return BGTaskManagerSingleton.instance
    }
}