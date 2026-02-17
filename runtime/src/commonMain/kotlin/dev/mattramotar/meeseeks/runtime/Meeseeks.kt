package dev.mattramotar.meeseeks.runtime

public object Meeseeks {

    public fun initialize(context: AppContext, configure: ConfigurationScope.() -> Unit = {}): BGTaskManager {
        val configurationScope = ConfigurationScope(context)
        configurationScope.configure()
        return configurationScope.build()
    }
}
