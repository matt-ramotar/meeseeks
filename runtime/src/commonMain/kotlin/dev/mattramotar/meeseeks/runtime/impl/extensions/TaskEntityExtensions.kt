package dev.mattramotar.meeseeks.runtime.impl.extensions

import dev.mattramotar.meeseeks.runtime.Task
import dev.mattramotar.meeseeks.runtime.db.TaskEntity

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