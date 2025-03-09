package dev.mattramotar.meeseeks.runtime.dsl.task

import dev.mattramotar.meeseeks.runtime.types.BooleanValue
import dev.mattramotar.meeseeks.runtime.types.ListValue
import dev.mattramotar.meeseeks.runtime.types.NullValue
import dev.mattramotar.meeseeks.runtime.types.StringValue
import dev.mattramotar.meeseeks.runtime.types.Value

class ListValueBuilder {
    private val items = mutableListOf<Value>()

    operator fun Value.unaryPlus() {
        items += this
    }

    operator fun String.unaryPlus() {
        items += StringValue(this)
    }

    operator fun Boolean.unaryPlus() {
        items += BooleanValue(this)
    }

    operator fun NullValue.unaryPlus() {
        items += this
    }

    internal fun build() = ListValue(items.toList())
}