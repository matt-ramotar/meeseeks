package dev.mattramotar.meeseeks.runtime.impl

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.Foundation.NSProcessInfo

@OptIn(ExperimentalForeignApi::class)
fun isIOS16OrLater(): Boolean {
    val version = NSProcessInfo.processInfo.operatingSystemVersion()
    return version.useContents { majorVersion } >= 16
}