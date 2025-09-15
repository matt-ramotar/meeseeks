package dev.mattramotar.meeseeks.runtime.internal.db.adapters

import app.cash.sqldelight.ColumnAdapter
import dev.mattramotar.meeseeks.runtime.internal.db.model.BackoffPolicy

internal object BackoffPolicyAdapter : ColumnAdapter<BackoffPolicy, String> {
    override fun decode(databaseValue: String): BackoffPolicy = BackoffPolicy.valueOf(databaseValue)
    override fun encode(value: BackoffPolicy): String = value.name
}