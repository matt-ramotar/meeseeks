package dev.mattramotar.meeseeks.runtime.dsl.box


import dev.mattramotar.meeseeks.runtime.MeeseeksContext
import dev.mattramotar.meeseeks.runtime.MeeseeksRegistry
import dev.mattramotar.meeseeks.runtime.MeeseeksBox
import dev.mattramotar.meeseeks.runtime.MeeseeksBoxConfig
import dev.mattramotar.meeseeks.runtime.impl.MeeseeksBoxFactory


fun MeeseeksBox(
    context: MeeseeksContext,
    config: MeeseeksBoxConfig = MeeseeksBoxConfig(),
    registry: MeeseeksRegistry
): MeeseeksBox {

    val factory = MeeseeksBoxFactory()

    return factory.create(
        context,
        registry,
        config
    )
}



