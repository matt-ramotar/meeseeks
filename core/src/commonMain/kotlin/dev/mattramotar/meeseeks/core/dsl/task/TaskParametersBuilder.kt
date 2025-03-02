package dev.mattramotar.meeseeks.core.dsl.task

import dev.mattramotar.meeseeks.core.TaskParameters
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


class TaskParametersBuilder {
    private val map = mutableMapOf<String, Value>()


    infix fun String.to(value: String) {
        map[this] = StringValue(value)
    }


    infix fun String.to(value: Boolean) {
        map[this] = BooleanValue(value)
    }


    infix fun String.to(value: Int) {
        map[this] = IntValue(value)
    }


    infix fun String.to(value: Long) {
        map[this] = LongValue(value)
    }


    infix fun String.to(value: Float) {
        map[this] = FloatValue(value)
    }


    infix fun String.to(value: Double) {
        map[this] = DoubleValue(value)
    }


    infix fun String.to(value: Byte) {
        map[this] = ByteValue(value)
    }


    infix fun String.to(value: Short) {
        map[this] = ShortValue(value)
    }


    infix fun String.to(value: Char) {
        map[this] = CharValue(value)
    }

    infix fun String.to(value: NullValue) {
        map[this] = value
    }

    infix fun String.to(n: Nothing?) {
        map[this] = NullValue
    }

    infix fun String.to(value: Value) {
        map[this] = value
    }

    infix fun String.to(value: List<Value>) {
        map[this] = ListValue(value)
    }

    infix fun String.toList(builder: ListValueBuilder.() -> Unit) {
        map[this] = ListValueBuilder().apply(builder).build()
    }

    infix fun String.toMap(builder: MapValueBuilder.() -> Unit) {
        map[this] = MapValueBuilder().apply(builder).build()
    }

    internal fun build(): TaskParameters = TaskParameters(map)
}