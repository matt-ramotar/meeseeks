package dev.mattramotar.meeseeks.core.dsl.box


import dev.mattramotar.meeseeks.core.MeeseeksContext
import dev.mattramotar.meeseeks.core.MeeseeksRegistry
import dev.mattramotar.meeseeks.core.MrMeeseeksBox
import dev.mattramotar.meeseeks.core.MrMeeseeksBoxConfig
import dev.mattramotar.meeseeks.core.impl.MrMeeseeksBoxFactory


fun MrMeeseeksBox(
    context: MeeseeksContext,
    config: MrMeeseeksBoxConfig = MrMeeseeksBoxConfig(),
    registryBuilder: MeeseeksRegistry.Builder.() -> Unit
): MrMeeseeksBox {

    val registry = MeeseeksRegistry.Builder().apply(registryBuilder).build()

    val factory = MrMeeseeksBoxFactory()

    return factory.create(
        context,
        registry,
        config
    )
}



