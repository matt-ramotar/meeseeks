package dev.mattramotar.meeseeks.runtime.dsl.task

import dev.mattramotar.meeseeks.runtime.TaskParameters
import dev.mattramotar.meeseeks.runtime.types.Value


fun taskParametersOf(
    vararg pairs: Pair<String, Value>
): TaskParameters = TaskParameters(mapOf(*pairs))

/**
 * @sample taskParameters
 */
fun taskParametersOf(
    builder: TaskParametersBuilder.() -> Unit
): TaskParameters {
    return TaskParametersBuilder().apply(builder).build()
}

private val taskParameters = taskParametersOf {
    "key" to 'v'
    "key" to "value"

    "key" to 1
    "key" to 1.toFloat()
    "key" to 1.toDouble()
    "key" to 1.toByte()
    "key" to 1.toShort()
    "key" to 1.toLong()

    "key" to true
    "key" to null

    "key" toList { 'v' + "value" + 1 + true + null }

    "key" toMap {
        "key" to 'v'
        "key" to "value"
        "key" to 1
        "key" to true
        "key" to null
    }
}

