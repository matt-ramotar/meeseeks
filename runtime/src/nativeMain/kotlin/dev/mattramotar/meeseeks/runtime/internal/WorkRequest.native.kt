package dev.mattramotar.meeseeks.runtime.internal

import dev.mattramotar.meeseeks.runtime.TaskId

/**
 * Largely symbolic on native, as iOS uses fixed identifiers.
 */
internal actual class WorkRequest(
    /**
     * We are storing the internal [TaskId.value], but it's not used by the scheduler.
     */
    actual val id: String
)