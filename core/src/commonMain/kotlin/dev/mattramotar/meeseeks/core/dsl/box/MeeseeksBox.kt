package dev.mattramotar.meeseeks.core.dsl.box


import dev.mattramotar.meeseeks.core.MeeseeksContext
import dev.mattramotar.meeseeks.core.MeeseeksRegistry
import dev.mattramotar.meeseeks.core.MeeseeksBox
import dev.mattramotar.meeseeks.core.MeeseeksBoxConfig
import dev.mattramotar.meeseeks.core.impl.MeeseeksBoxFactory


fun MeeseeksBox(
    context: MeeseeksContext,
    config: MeeseeksBoxConfig = MeeseeksBoxConfig(),
    registryBuilder: MeeseeksRegistry.Builder.() -> Unit
): MeeseeksBox {

    val registry = MeeseeksRegistry.Builder().apply(registryBuilder).build()

    val factory = MeeseeksBoxFactory()

    return factory.create(
        context,
        registry,
        config
    )
}



