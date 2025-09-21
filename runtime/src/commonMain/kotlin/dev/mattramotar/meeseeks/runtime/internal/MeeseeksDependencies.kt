package dev.mattramotar.meeseeks.runtime.internal

import dev.mattramotar.meeseeks.runtime.AppContext
import dev.mattramotar.meeseeks.runtime.BGTaskManagerConfig
import dev.mattramotar.meeseeks.runtime.db.MeeseeksDatabase

/**
 * Container for core dependencies needed by platform workers.
 */
internal data class MeeseeksDependencies(
    val database: MeeseeksDatabase,
    val registry: WorkerRegistry,
    val config: BGTaskManagerConfig,
    val appContext: AppContext
)