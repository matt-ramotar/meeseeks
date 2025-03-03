package dev.mattramotar.meeseeks.core.impl

import dev.mattramotar.meeseeks.core.MeeseeksContext
import dev.mattramotar.meeseeks.core.MeeseeksRegistry
import dev.mattramotar.meeseeks.core.MeeseeksBox
import dev.mattramotar.meeseeks.core.MeeseeksBoxConfig

internal expect class MeeseeksBoxFactory() {
    fun create(
        context: MeeseeksContext,
        registry: MeeseeksRegistry,
        config: MeeseeksBoxConfig = MeeseeksBoxConfig()
    ): MeeseeksBox
}