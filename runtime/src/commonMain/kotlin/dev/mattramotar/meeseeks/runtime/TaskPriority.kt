package dev.mattramotar.meeseeks.runtime

import dev.mattramotar.meeseeks.runtime.TaskPriority.HIGH
import dev.mattramotar.meeseeks.runtime.TaskPriority.LOW
import dev.mattramotar.meeseeks.runtime.TaskPriority.MEDIUM
import kotlinx.serialization.Serializable

/**
 * Influences the order in which tasks are executed when multiple are pending.
 *
 * @property LOW Execution can be scheduled behind other work.
 * @property MEDIUM Execution should be scheduled as normal.
 * @property HIGH Execution should be scheduled in front of other work.
 */
@Serializable
enum class TaskPriority {
    LOW,
    MEDIUM,
    HIGH
}