package dev.mattramotar.meeseeks.runtime

object Meeseeks {

    fun initialize(context: AppContext, configure: ConfigurationScope.() -> Unit = {}): BGTaskManager {
        val configurationScope = ConfigurationScope(context)
        configurationScope.configure()
        return configurationScope.build()
    }
}