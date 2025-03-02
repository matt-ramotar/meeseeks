package dev.mattramotar.meeseeks.core.impl.db.adapters

import dev.mattramotar.meeseeks.core.db.TaskLogEntity
import kotlinx.serialization.json.Json

internal fun taskLogEntityAdapter(json: Json = Json): TaskLogEntity.Adapter = TaskLogEntity.Adapter(
    resultAdapter = TaskResultAdapter(json)
)