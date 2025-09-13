package dev.mattramotar.meeseeks.runtime

import android.content.Context

actual interface AppContext {
    val applicationContext: Context
}