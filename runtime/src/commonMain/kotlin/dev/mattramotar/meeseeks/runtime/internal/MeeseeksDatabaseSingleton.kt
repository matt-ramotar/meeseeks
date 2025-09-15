package dev.mattramotar.meeseeks.runtime.internal

import dev.mattramotar.meeseeks.runtime.AppContext
import dev.mattramotar.meeseeks.runtime.db.MeeseeksDatabase
import dev.mattramotar.meeseeks.runtime.internal.concurrency.synchronized
import dev.mattramotar.meeseeks.runtime.internal.db.adapters.TaskSpecAdapter
import dev.mattramotar.meeseeks.runtime.internal.db.adapters.taskLogEntityAdapter
import kotlinx.serialization.json.Json
import kotlin.concurrent.Volatile


internal object MeeseeksDatabaseSingleton {

    @Volatile
    private var _instance: MeeseeksDatabase? = null

    val instance: MeeseeksDatabase
        get() = require()


    fun init(context: AppContext, json: Json) {
        if (_instance == null) {
            synchronized(this) {
                if (_instance == null) {
                    val factory = MeeseeksDatabaseFactory()
                    _instance = factory.create(
                        context = context,
                        taskLogAdapter = taskLogEntityAdapter(json),
                        taskSpecAdapter = TaskSpecAdapter
                    )
                }
            }
        }
    }

    private fun require(): MeeseeksDatabase {
        var retriesRemaining = 3
        var singleton: MeeseeksDatabase? = null

        while (retriesRemaining > 0 && singleton == null) {
            singleton = _instance
            retriesRemaining--
        }

        return singleton ?: throw IllegalStateException("MeeseeksDatabase not available.")
    }
}