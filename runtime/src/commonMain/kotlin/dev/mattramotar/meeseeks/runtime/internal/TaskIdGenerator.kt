package dev.mattramotar.meeseeks.runtime.internal

import dev.mattramotar.meeseeks.runtime.TaskId

internal fun newTaskId(): TaskId = TaskId(generateTaskIdValue())

internal expect fun generateTaskIdValue(): String
