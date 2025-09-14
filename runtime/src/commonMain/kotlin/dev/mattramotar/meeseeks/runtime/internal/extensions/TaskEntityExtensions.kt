package dev.mattramotar.meeseeks.runtime.internal.extensions

import dev.mattramotar.meeseeks.runtime.TaskRequest
import dev.mattramotar.meeseeks.runtime.db.TaskEntity

internal object TaskEntityExtensions {
    fun TaskEntity.toTaskRequest(): TaskRequest = TaskRequest(
        payload,
        preconditions,
        priority,
        schedule,
        retryPolicy
    )
}