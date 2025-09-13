package dev.mattramotar.meeseeks.runtime.dsl.registry

import dev.mattramotar.meeseeks.runtime.TaskWorkerRegistry
import dev.mattramotar.meeseeks.runtime.TaskWorker
import dev.mattramotar.meeseeks.runtime.Task

object MeeseeksRegistryBuilderExtensions {
    fun TaskWorkerRegistry.Builder.register(
        type: String,
        createTaskWorker: (Task) -> TaskWorker
    ) {
        registerFactory(type) {
            createTaskWorker(it)
        }
    }
}