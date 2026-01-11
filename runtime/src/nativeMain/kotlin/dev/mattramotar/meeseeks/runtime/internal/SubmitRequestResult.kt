package dev.mattramotar.meeseeks.runtime.internal

/**
 * Result of submitting a BGTaskRequest to the iOS BGTaskScheduler.
 *
 * @see <a href="https://developer.apple.com/documentation/backgroundtasks/bgtaskscheduler/submit(_:)">BGTaskScheduler.submit(_:)</a>
 */
internal sealed class SubmitRequestResult {

    /**
     * The task request was successfully submitted to the scheduler.
     */
    data object Success : SubmitRequestResult()

    /**
     * The task request failed to submit.
     *
     * @see <a href="https://developer.apple.com/documentation/backgroundtasks/bgtaskscheduler/error">BGTaskScheduler.Error</a>
     */
    sealed class Failure : SubmitRequestResult() {
        abstract val errorCode: Int
        abstract val errorDescription: String?

        /**
         * Transient failure that may succeed on retry.
         *
         * @see <a href="https://developer.apple.com/documentation/backgroundtasks/bgtaskscheduler/error/code/toomanypendingtaskrequests">BGTaskScheduler.Error.Code.tooManyPendingTaskRequests</a>
         */
        data class Retriable(
            override val errorCode: Int,
            override val errorDescription: String?
        ) : Failure()

        /**
         * Permanent failure that will not succeed on retry.
         *
         * @see <a href="https://developer.apple.com/documentation/backgroundtasks/bgtaskscheduler/error/code/unavailable">BGTaskScheduler.Error.Code.unavailable</a>
         * @see <a href="https://developer.apple.com/documentation/backgroundtasks/bgtaskscheduler/error/code/notpermitted">BGTaskScheduler.Error.Code.notPermitted</a>
         */
        data class NonRetriable(
            override val errorCode: Int,
            override val errorDescription: String?
        ) : Failure()
    }
}
