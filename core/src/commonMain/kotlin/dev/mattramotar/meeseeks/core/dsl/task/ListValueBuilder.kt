package dev.mattramotar.meeseeks.core.dsl.task

import dev.mattramotar.meeseeks.core.types.BooleanValue
import dev.mattramotar.meeseeks.core.types.ByteValue
import dev.mattramotar.meeseeks.core.types.CharValue
import dev.mattramotar.meeseeks.core.types.DoubleValue
import dev.mattramotar.meeseeks.core.types.FloatValue
import dev.mattramotar.meeseeks.core.types.IntValue
import dev.mattramotar.meeseeks.core.types.ListValue
import dev.mattramotar.meeseeks.core.types.LongValue
import dev.mattramotar.meeseeks.core.types.NullValue
import dev.mattramotar.meeseeks.core.types.ShortValue
import dev.mattramotar.meeseeks.core.types.StringValue
import dev.mattramotar.meeseeks.core.types.Value

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

    operator fun Int.unaryPlus() {
        items += IntValue(this)
    }

    operator fun Long.unaryPlus() {
        items += LongValue(this)
    }

    operator fun Float.unaryPlus() {
        items += FloatValue(this)
    }

    operator fun Double.unaryPlus() {
        items += DoubleValue(this)
    }

    operator fun Byte.unaryPlus() {
        items += ByteValue(this)
    }

    operator fun Short.unaryPlus() {
        items += ShortValue(this)
    }

    operator fun Char.unaryPlus() {
        items += CharValue(this)
    }

    operator fun NullValue.unaryPlus() {
        items += this
    }

    internal fun build() = ListValue(items.toList())
}