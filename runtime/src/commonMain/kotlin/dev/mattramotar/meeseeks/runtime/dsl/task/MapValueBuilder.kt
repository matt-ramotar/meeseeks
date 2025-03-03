package dev.mattramotar.meeseeks.runtime.dsl.task

import dev.mattramotar.meeseeks.runtime.types.BooleanValue
import dev.mattramotar.meeseeks.runtime.types.ByteValue
import dev.mattramotar.meeseeks.runtime.types.CharValue
import dev.mattramotar.meeseeks.runtime.types.DoubleValue
import dev.mattramotar.meeseeks.runtime.types.FloatValue
import dev.mattramotar.meeseeks.runtime.types.IntValue
import dev.mattramotar.meeseeks.runtime.types.LongValue
import dev.mattramotar.meeseeks.runtime.types.MapValue
import dev.mattramotar.meeseeks.runtime.types.NullValue
import dev.mattramotar.meeseeks.runtime.types.ShortValue
import dev.mattramotar.meeseeks.runtime.types.StringValue
import dev.mattramotar.meeseeks.runtime.types.Value

class MapValueBuilder {
    private val pairs = mutableMapOf<String, Value>()

    infix fun String.to(value: String) {
        pairs[this] = StringValue(value)
    }

    infix fun String.to(value: Boolean) {
        pairs[this] = BooleanValue(value)
    }

    infix fun String.to(value: Int) {
        pairs[this] = IntValue(value)
    }

    infix fun String.to(value: Long) {
        pairs[this] = LongValue(value)
    }

    infix fun String.to(value: Float) {
        pairs[this] = FloatValue(value)
    }

    infix fun String.to(value: Double) {
        pairs[this] = DoubleValue(value)
    }

    infix fun String.to(value: Byte) {
        pairs[this] = ByteValue(value)
    }

    infix fun String.to(value: Short) {
        pairs[this] = ShortValue(value)
    }

    infix fun String.to(value: Char) {
        pairs[this] = CharValue(value)
    }

    infix fun String.to(value: NullValue) {
        pairs[this] = value
    }

    infix fun String.to(value: Value) {
        pairs[this] = value
    }

    internal fun build(): MapValue = MapValue(pairs)
}