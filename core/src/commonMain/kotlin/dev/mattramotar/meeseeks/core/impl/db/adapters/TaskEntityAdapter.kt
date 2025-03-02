package dev.mattramotar.meeseeks.core.impl.db.adapters

import dev.mattramotar.meeseeks.core.db.TaskEntity
import kotlinx.serialization.json.Json

internal fun taskEntityAdapter(json: Json = Json) = TaskEntity.Adapter(
    preconditionsAdapter = TaskPreconditionsAdapter(json),
    priorityAdapter = TaskPriorityAdapter(json),
    scheduleAdapter = TaskScheduleAdapter(json),
    retryPolicyAdapter = TaskRetryPolicyAdapter(json),
    statusAdapter = TaskStatusAdapter(json),
    parametersAdapter = TaskParametersAdapter(json)
)