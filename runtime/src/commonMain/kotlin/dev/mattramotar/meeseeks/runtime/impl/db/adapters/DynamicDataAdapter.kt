package dev.mattramotar.meeseeks.runtime.impl.db.adapters

import app.cash.sqldelight.ColumnAdapter
import dev.mattramotar.meeseeks.runtime.DynamicData
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal class DynamicDataAdapter(
    private val json: Json
) : ColumnAdapter<DynamicData, String> {
    override fun decode(databaseValue: String): DynamicData {
        return json.decodeFromString(databaseValue)
    }

    override fun encode(value: DynamicData): String {
        return json.encodeToString(value)
    }
}