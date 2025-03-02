package dev.mattramotar.meeseeks.core

import dev.mattramotar.meeseeks.core.TaskPriority.HIGH
import dev.mattramotar.meeseeks.core.TaskPriority.LOW
import dev.mattramotar.meeseeks.core.TaskPriority.MEDIUM
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