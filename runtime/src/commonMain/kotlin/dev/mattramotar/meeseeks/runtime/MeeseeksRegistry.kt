package dev.mattramotar.meeseeks.runtime


class MeeseeksRegistry private constructor(
    private val factories: Map<String, MrMeeseeksFactory>
) {

    internal fun getFactory(type: String): MrMeeseeksFactory =
        factories[type] ?: error("MrMeeseeksFactory not found for type $type.")

    companion object {
        fun build(block: Builder.() -> Unit): MeeseeksRegistry {
            return Builder().apply(block).build()
        }
    }

    class Builder {
        private val factories = mutableMapOf<String, MrMeeseeksFactory>()
        fun registerFactory(type: String, factory: MrMeeseeksFactory) {
            factories[type] = factory
        }

        fun build(): MeeseeksRegistry = MeeseeksRegistry(factories)
    }
}