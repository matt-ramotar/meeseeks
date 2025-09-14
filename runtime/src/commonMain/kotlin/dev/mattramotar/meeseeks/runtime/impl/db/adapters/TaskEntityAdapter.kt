package dev.mattramotar.meeseeks.runtime.impl.db.adapters

import dev.mattramotar.meeseeks.runtime.db.TaskEntity
import kotlinx.serialization.json.Json

internal fun taskEntityAdapter(json: Json = Json) = TaskEntity.Adapter(
    preconditionsAdapter = TaskPreconditionsAdapter(json),
    priorityAdapter = TaskPriorityAdapter(json),
    scheduleAdapter = TaskScheduleAdapter(json),
    retryPolicyAdapter = TaskRetryPolicyAdapter(json),
    statusAdapter = TaskStatusAdapter(json),
    payloadAdapter = TaskPayloadAdapter(json),
)