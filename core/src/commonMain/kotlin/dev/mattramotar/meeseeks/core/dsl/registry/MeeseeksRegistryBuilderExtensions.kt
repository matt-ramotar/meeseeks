package dev.mattramotar.meeseeks.core.dsl.registry

import dev.mattramotar.meeseeks.core.MeeseeksRegistry
import dev.mattramotar.meeseeks.core.MrMeeseeks
import dev.mattramotar.meeseeks.core.Task

object MeeseeksRegistryBuilderExtensions {
    fun MeeseeksRegistry.Builder.register(
        type: String,
        createMrMeeseeks: (Task) -> MrMeeseeks
    ) {
        registerFactory(type) {
            createMrMeeseeks(it)
        }
    }
}