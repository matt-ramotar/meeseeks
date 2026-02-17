package dev.mattramotar.meeseeks.runtime

/**
 * Task outcome.
 */
public sealed class TaskResult {

    internal abstract val type: Type

    public data object Success : TaskResult() {
        override val type = Type.Success
    }

    public data object Retry : TaskResult() {
        override val type = Type.Retry
    }

    public sealed class Failure : TaskResult() {
        public abstract val error: Throwable?

        public data class Permanent(
            override val error: Throwable? = null,
        ) : Failure() {
            override val type = Type.PermanentFailure
        }

        public data class Transient(
            override val error: Throwable? = null,
        ) : Failure() {
            override val type = Type.TransientFailure
        }
    }

    public enum class Type {
        Success,
        Retry,
        PermanentFailure,
        TransientFailure,
        SuccessAndScheduledNext, // For native runner logs
    }
}
