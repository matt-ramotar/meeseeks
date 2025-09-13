package dev.mattramotar.meeseeks.runtime.dsl.registry

import dev.mattramotar.meeseeks.runtime.MeeseeksRegistry
import dev.mattramotar.meeseeks.runtime.TaskWorker
import dev.mattramotar.meeseeks.runtime.Task

object MeeseeksRegistryBuilderExtensions {
    fun MeeseeksRegistry.Builder.register(
        type: String,
        createTaskWorker: (Task) -> TaskWorker
    ) {
        registerFactory(type) {
            createTaskWorker(it)
        }
    }
}