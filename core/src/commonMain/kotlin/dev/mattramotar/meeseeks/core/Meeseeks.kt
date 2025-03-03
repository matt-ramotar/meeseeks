package dev.mattramotar.meeseeks.core

import dev.mattramotar.meeseeks.core.impl.MeeseeksBoxSingleton

object Meeseeks {

    fun initialize(
        context: MeeseeksContext,
        config: MeeseeksBoxConfig = MeeseeksBoxConfig(),
        registryBuilder: MeeseeksRegistry.Builder.() -> Unit
    ) {
        MeeseeksBoxSingleton.getOrCreateMeeseeksBox(context, config, registryBuilder)
    }
}

