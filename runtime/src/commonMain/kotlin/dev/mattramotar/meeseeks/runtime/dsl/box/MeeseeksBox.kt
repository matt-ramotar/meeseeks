package dev.mattramotar.meeseeks.runtime.dsl.box


import dev.mattramotar.meeseeks.runtime.MeeseeksContext
import dev.mattramotar.meeseeks.runtime.MeeseeksRegistry
import dev.mattramotar.meeseeks.runtime.MeeseeksBox
import dev.mattramotar.meeseeks.runtime.MeeseeksBoxConfig
import dev.mattramotar.meeseeks.runtime.impl.MeeseeksBoxFactory


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



