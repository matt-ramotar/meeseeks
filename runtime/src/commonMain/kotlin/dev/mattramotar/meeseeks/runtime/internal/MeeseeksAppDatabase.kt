package dev.mattramotar.meeseeks.runtime.internal

import dev.mattramotar.meeseeks.runtime.AppContext
import dev.mattramotar.meeseeks.runtime.internal.concurrency.synchronized
import dev.mattramotar.meeseeks.runtime.db.MeeseeksDatabase
import dev.mattramotar.meeseeks.runtime.internal.db.adapters.taskEntityAdapter
import dev.mattramotar.meeseeks.runtime.internal.db.adapters.taskLogEntityAdapter
import kotlinx.serialization.json.Json
import kotlin.concurrent.Volatile


internal object MeeseeksAppDatabase {

    @Volatile
    private var singleton: MeeseeksDatabase? = null

    fun init(context: AppContext, json: Json) {
        if (singleton == null) {
            synchronized(this) {
                if (singleton == null) {
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

    fun require(context: AppContext, json: Json): MeeseeksDatabase {
        init(context, json)

        var retriesRemaining = 3
        var singleton: MeeseeksDatabase? = null

        while (retriesRemaining > 0 && singleton == null) {
            singleton = MeeseeksAppDatabase.singleton
            retriesRemaining--
        }

        return singleton ?: throw IllegalStateException("MeeseeksDatabase not available.")
    }
}