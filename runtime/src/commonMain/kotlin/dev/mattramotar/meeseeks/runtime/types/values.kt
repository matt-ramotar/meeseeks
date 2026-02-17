package dev.mattramotar.meeseeks.runtime.types

import kotlinx.serialization.Serializable

@Serializable
public sealed class Value

@Serializable
public sealed class Primitive : Value()

@Serializable
public data class CharValue(public val value: Char) : Primitive()

@Serializable
public data class StringValue(public val value: String) : Primitive()

@Serializable
public data class BooleanValue(public val value: Boolean) : Primitive()

@Serializable
public data class IntValue(public val value: Int) : Primitive()

@Serializable
public data class LongValue(public val value: Long) : Primitive()

@Serializable
public data class FloatValue(public val value: Float) : Primitive()

@Serializable
public data class DoubleValue(public val value: Double) : Primitive()

@Serializable
public data class ByteValue(public val value: Byte) : Primitive()

@Serializable
public data class ShortValue(public val value: Short) : Primitive()

@Serializable
public data object NullValue : Primitive()

@Serializable
public sealed class Collection : Value()

@Serializable
public data class ListValue(public val value: List<Value>) : Collection()

@Serializable
public data class MapValue(public val value: Map<String, Value>) : Collection()

internal fun Any?.wrapped(): Value = when (this) {
    null -> NullValue
    is Value -> this
    is String -> StringValue(this)
    is Boolean -> BooleanValue(this)
    is Int -> IntValue(this)
    is Long -> LongValue(this)
    is Float -> FloatValue(this)
    is Double -> DoubleValue(this)
    is Byte -> ByteValue(this)
    is Short -> ShortValue(this)
    is Char -> CharValue(this)
    is Map<*, *> -> MapValue(this.entries.mapNotNull { (k, v) ->
        (k as? String)?.let { it to v.wrapped() }
    }.toMap())
    is List<*> -> ListValue(this.map { it.wrapped() })
    else -> StringValue(this.toString())
}
