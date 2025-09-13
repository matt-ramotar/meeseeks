package dev.mattramotar.meeseeks.runtime


class MeeseeksRegistry private constructor(
    private val factories: Map<String, TaskWorkerFactory>
) {

    internal fun getFactory(type: String): TaskWorkerFactory =
        factories[type] ?: error("MrMeeseeksFactory not found for type $type.")

    companion object {
        fun build(block: Builder.() -> Unit): MeeseeksRegistry {
            return Builder().apply(block).build()
        }
    }

    class Builder {
        private val factories = mutableMapOf<String, TaskWorkerFactory>()
        fun registerFactory(type: String, factory: TaskWorkerFactory) {
            factories[type] = factory
        }

        fun build(): MeeseeksRegistry = MeeseeksRegistry(factories)
    }
}