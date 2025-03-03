package dev.mattramotar.meeseeks.core.impl

import dev.mattramotar.meeseeks.core.MeeseeksBox
import dev.mattramotar.meeseeks.core.MeeseeksBoxConfig
import dev.mattramotar.meeseeks.core.MeeseeksContext
import dev.mattramotar.meeseeks.core.MeeseeksRegistry
import dev.mattramotar.meeseeks.core.dsl.box.MeeseeksBox
import dev.mattramotar.meeseeks.core.impl.concurrency.synchronized
import kotlin.concurrent.Volatile

internal object MeeseeksBoxSingleton {

    @Volatile
    private var _meeseeksBox: MeeseeksBox? = null

    val meeseeksBox: MeeseeksBox
        get() {
            return _meeseeksBox ?: throw IllegalStateException("MeeseeksBox not set.")
        }

    fun getOrCreateMeeseeksBox(
        context: MeeseeksContext,
        config: MeeseeksBoxConfig = MeeseeksBoxConfig(),
        registryBuilder: MeeseeksRegistry.Builder.() -> Unit
    ): MeeseeksBox {
        val existingBoxCheck1 = _meeseeksBox
        if (existingBoxCheck1 != null) return existingBoxCheck1

        return synchronized(this) {
            val existingBoxCheck2 = _meeseeksBox
            if (existingBoxCheck2 != null) {
                existingBoxCheck2
            } else {
                val newBox = MeeseeksBox(context, config, registryBuilder)
                _meeseeksBox = newBox
                newBox
            }
        }
    }


    fun resetState() {
        synchronized(this) {
            _meeseeksBox = null
        }
    }
}

