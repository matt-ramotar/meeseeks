package dev.mattramotar.meeseeks.runtime

import dev.mattramotar.meeseeks.runtime.impl.MeeseeksBoxSingleton

object Meeseeks {

    fun initialize(
        context: MeeseeksContext,
        config: MeeseeksBoxConfig = MeeseeksBoxConfig(),
        registryBuilder: MeeseeksRegistry.Builder.() -> Unit
    ) {
        MeeseeksBoxSingleton.getOrCreateMeeseeksBox(context, config, registryBuilder)
    }
}

