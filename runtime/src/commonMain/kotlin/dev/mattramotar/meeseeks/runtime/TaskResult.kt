package dev.mattramotar.meeseeks.runtime

/**
 * Task outcome.
 */
sealed class TaskResult {

    internal abstract val type: Type

    data object Success : TaskResult() {
        override val type = Type.Success
    }

    data object Retry : TaskResult() {
        override val type = Type.Retry
    }

    sealed class Failure : TaskResult() {
        abstract val error: Throwable?

        data class Permanent(
            override val error: Throwable? = null
        ) : Failure() {
            override val type = Type.PermanentFailure
        }

        data class Transient(
            override val error: Throwable? = null
        ) : Failure() {
            override val type = Type.TransientFailure
        }
    }

    enum class Type {
        Success,
        Retry,
        PermanentFailure,
        TransientFailure,
        SuccessAndScheduledNext, // For native runner logs
    }
}

