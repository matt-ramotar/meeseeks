package dev.mattramotar.meeseeks.core

import dev.mattramotar.meeseeks.core.dsl.box.MrMeeseeksBox
import dev.mattramotar.meeseeks.core.impl.concurrency.synchronized
import kotlin.concurrent.Volatile

object Meeseeks {

    @Volatile
    private var mrMeeseeksBox: MrMeeseeksBox? = null

    fun getOrCreateMrMeeseeksBox(
        context: MeeseeksContext,
        config: MrMeeseeksBoxConfig = MrMeeseeksBoxConfig(),
        registryBuilder: MeeseeksRegistry.Builder.() -> Unit
    ): MrMeeseeksBox {
        val existingBoxCheck1 = mrMeeseeksBox
        if (existingBoxCheck1 != null) return existingBoxCheck1

        return synchronized(this) {
            val existingBoxCheck2 = mrMeeseeksBox
            if (existingBoxCheck2 != null) {
                existingBoxCheck2
            } else {
                val newBox = MrMeeseeksBox(context, config, registryBuilder)
                mrMeeseeksBox = newBox
                newBox
            }
        }
    }

    fun clearMrMeeseeksBox() {
        synchronized(this) {
            mrMeeseeksBox = null
        }
    }
}

