package dev.mattramotar.meeseeks.core.impl

import dev.mattramotar.meeseeks.core.MeeseeksContext
import dev.mattramotar.meeseeks.core.MeeseeksRegistry
import dev.mattramotar.meeseeks.core.MrMeeseeksBox
import dev.mattramotar.meeseeks.core.MrMeeseeksBoxConfig

internal expect class MrMeeseeksBoxFactory() {
    fun create(
        context: MeeseeksContext,
        registry: MeeseeksRegistry,
        config: MrMeeseeksBoxConfig = MrMeeseeksBoxConfig()
    ): MrMeeseeksBox
}