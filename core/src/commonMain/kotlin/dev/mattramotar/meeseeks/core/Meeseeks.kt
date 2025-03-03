package dev.mattramotar.meeseeks.core

import dev.mattramotar.meeseeks.core.dsl.box.MeeseeksBox
import dev.mattramotar.meeseeks.core.impl.concurrency.synchronized
import kotlin.concurrent.Volatile

object Meeseeks {

    @Volatile
    private var meeseeksBox: MeeseeksBox? = null

    fun getOrCreateMeeseeksBox(
        context: MeeseeksContext,
        config: MeeseeksBoxConfig = MeeseeksBoxConfig(),
        registryBuilder: MeeseeksRegistry.Builder.() -> Unit
    ): MeeseeksBox {
        val existingBoxCheck1 = meeseeksBox
        if (existingBoxCheck1 != null) return existingBoxCheck1

        return synchronized(this) {
            val existingBoxCheck2 = meeseeksBox
            if (existingBoxCheck2 != null) {
                existingBoxCheck2
            } else {
                val newBox = MeeseeksBox(context, config, registryBuilder)
                meeseeksBox = newBox
                newBox
            }
        }
    }

    fun clearMeeseeksBox() {
        synchronized(this) {
            meeseeksBox = null
        }
    }
}

