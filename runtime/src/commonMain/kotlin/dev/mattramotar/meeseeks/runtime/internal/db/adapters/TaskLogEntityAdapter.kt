package dev.mattramotar.meeseeks.runtime.internal.db.adapters

import dev.mattramotar.meeseeks.runtime.db.TaskLogEntity
import kotlinx.serialization.json.Json

internal fun taskLogEntityAdapter(json: Json = Json): TaskLogEntity.Adapter = TaskLogEntity.Adapter(
    resultAdapter = TaskResultAdapter(json)
)