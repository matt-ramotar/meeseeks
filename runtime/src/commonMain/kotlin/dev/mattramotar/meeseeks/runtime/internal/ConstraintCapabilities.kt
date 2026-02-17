package dev.mattramotar.meeseeks.runtime.internal

import dev.mattramotar.meeseeks.runtime.TaskPreconditions

internal data class ConstraintCapabilities(
    val targetName: String,
    val supportsNetwork: Boolean,
    val supportsCharging: Boolean,
    val supportsBatteryNotLow: Boolean
) {

    fun validate(preconditions: TaskPreconditions, operation: String) {
        val unsupported = mutableListOf<String>()

        if (preconditions.requiresNetwork && !supportsNetwork) {
            unsupported += "requiresNetwork"
        }
        if (preconditions.requiresCharging && !supportsCharging) {
            unsupported += "requiresCharging"
        }
        if (preconditions.requiresBatteryNotLow && !supportsBatteryNotLow) {
            unsupported += "requiresBatteryNotLow"
        }

        if (unsupported.isEmpty()) {
            return
        }

        throw IllegalArgumentException(
            "Cannot $operation task on $targetName: unsupported preconditions " +
                unsupported.joinToString(prefix = "[", postfix = "]") +
                ". Supported on $targetName: ${supportedFlagsSummary()}."
        )
    }

    private fun supportedFlagsSummary(): String {
        val supported = buildList {
            if (supportsNetwork) add("requiresNetwork")
            if (supportsCharging) add("requiresCharging")
            if (supportsBatteryNotLow) add("requiresBatteryNotLow")
        }

        return if (supported.isEmpty()) "none" else supported.joinToString()
    }

    companion object {
        val ANDROID = ConstraintCapabilities(
            targetName = "Android",
            supportsNetwork = true,
            supportsCharging = true,
            supportsBatteryNotLow = true
        )

        val JVM = ConstraintCapabilities(
            targetName = "JVM",
            supportsNetwork = false,
            supportsCharging = false,
            supportsBatteryNotLow = false
        )

        val JS = ConstraintCapabilities(
            targetName = "JS",
            supportsNetwork = false,
            supportsCharging = false,
            supportsBatteryNotLow = false
        )

        val IOS = ConstraintCapabilities(
            targetName = "iOS",
            supportsNetwork = true,
            supportsCharging = true,
            supportsBatteryNotLow = false
        )
    }
}
