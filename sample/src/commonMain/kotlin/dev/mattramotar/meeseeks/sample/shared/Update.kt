package dev.mattramotar.meeseeks.sample.shared

import kotlinx.serialization.Serializable


@Serializable
data class Update(
    val token: String,
)


