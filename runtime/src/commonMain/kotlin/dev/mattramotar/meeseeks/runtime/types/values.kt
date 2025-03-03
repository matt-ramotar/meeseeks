package dev.mattramotar.meeseeks.runtime.types

import kotlinx.serialization.Serializable

@Serializable
sealed class Value

@Serializable
sealed class Primitive : Value()

@Serializable
data class CharValue(val value: Char) : Primitive()

@Serializable
data class StringValue(val value: String) : Primitive()

@Serializable
data class BooleanValue(val value: Boolean) : Primitive()

@Serializable
data class IntValue(val value: Int) : Primitive()

@Serializable
data class LongValue(val value: Long) : Primitive()

@Serializable
data class FloatValue(val value: Float) : Primitive()

@Serializable
data class DoubleValue(val value: Double) : Primitive()

@Serializable
data class ByteValue(val value: Byte) : Primitive()

@Serializable
data class ShortValue(val value: Short) : Primitive()

@Serializable
data object NullValue : Primitive()

@Serializable
sealed class Collection : Value()

@Serializable
data class ListValue(val value: List<Value>) : Collection()

@Serializable
data class MapValue(val value: Map<String, Value>) : Collection()