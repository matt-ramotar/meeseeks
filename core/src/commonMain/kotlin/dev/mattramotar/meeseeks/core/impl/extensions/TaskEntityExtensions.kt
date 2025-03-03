package dev.mattramotar.meeseeks.core.impl.extensions

import dev.mattramotar.meeseeks.core.Task
import dev.mattramotar.meeseeks.core.db.TaskEntity

internal object TaskEntityExtensions {
    fun TaskEntity.toTask(): Task = Task(
        meeseeksType,
        parameters,
        preconditions,
        priority,
        schedule,
        retryPolicy
    )
}