package dev.mattramotar.meeseeks.runtime.dsl.registry

import dev.mattramotar.meeseeks.runtime.MeeseeksRegistry
import dev.mattramotar.meeseeks.runtime.MrMeeseeks
import dev.mattramotar.meeseeks.runtime.Task

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