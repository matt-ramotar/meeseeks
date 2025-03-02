package dev.mattramotar.meeseeks.core.impl

import dev.mattramotar.meeseeks.core.MeeseeksContext
import dev.mattramotar.meeseeks.core.impl.concurrency.synchronized
import dev.mattramotar.meeseeks.core.db.MeeseeksDatabase
import dev.mattramotar.meeseeks.core.impl.db.adapters.taskEntityAdapter
import dev.mattramotar.meeseeks.core.impl.db.adapters.taskLogEntityAdapter
import kotlinx.serialization.json.Json
import kotlin.concurrent.Volatile


internal object MeeseeksAppDatabase {

    @Volatile
    private var singleton: MeeseeksDatabase? = null

    private fun init(context: MeeseeksContext) {
        if (singleton == null) {
            synchronized(this) {
                if (singleton == null) {
                    val json = Json {
                        ignoreUnknownKeys = true
                        isLenient = true
                    }
                    val factory = MeeseeksDatabaseFactory()
                    singleton = factory.create(
                        context = context,
                        taskAdapter = taskEntityAdapter(json),
                        taskLogAdapter = taskLogEntityAdapter(json)
                    )
                }
            }
        }
    }

    fun require(context: MeeseeksContext): MeeseeksDatabase {
        init(context)

        var retriesRemaining = 3
        var singleton: MeeseeksDatabase? = null

        while (retriesRemaining > 0 && singleton == null) {
            singleton = MeeseeksAppDatabase.singleton
            retriesRemaining--
        }

        return singleton ?: throw IllegalStateException("MeeseeksDatabase not available.")
    }
}