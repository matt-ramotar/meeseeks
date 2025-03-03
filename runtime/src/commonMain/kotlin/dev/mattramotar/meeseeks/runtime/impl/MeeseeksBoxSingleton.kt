package dev.mattramotar.meeseeks.runtime.impl

import dev.mattramotar.meeseeks.runtime.MeeseeksBox
import dev.mattramotar.meeseeks.runtime.MeeseeksBoxConfig
import dev.mattramotar.meeseeks.runtime.MeeseeksContext
import dev.mattramotar.meeseeks.runtime.MeeseeksRegistry
import dev.mattramotar.meeseeks.runtime.dsl.box.MeeseeksBox
import dev.mattramotar.meeseeks.runtime.impl.concurrency.synchronized
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

