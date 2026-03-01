package dev.mattramotar.meeseeks.sample.demo

import dev.mattramotar.meeseeks.runtime.AppContext
import dev.mattramotar.meeseeks.runtime.RuntimeContext
import dev.mattramotar.meeseeks.runtime.TaskPayload
import dev.mattramotar.meeseeks.runtime.TaskResult
import dev.mattramotar.meeseeks.runtime.Worker
import kotlinx.serialization.Serializable

@Serializable
public data object SuccessPayload : TaskPayload

@Serializable
public data object RetryThenSuccessPayload : TaskPayload

@Serializable
public data object PermanentFailurePayload : TaskPayload

@Serializable
public data object HeartbeatPayload : TaskPayload

internal class SuccessWorker(appContext: AppContext) : Worker<SuccessPayload>(appContext) {
    override suspend fun run(payload: SuccessPayload, context: RuntimeContext): TaskResult {
        return TaskResult.Success
    }
}

internal class RetryThenSuccessWorker(appContext: AppContext) : Worker<RetryThenSuccessPayload>(appContext) {
    override suspend fun run(payload: RetryThenSuccessPayload, context: RuntimeContext): TaskResult {
        if (context.attemptCount <= 1) {
            return TaskResult.Retry
        }
        return TaskResult.Success
    }
}

internal class PermanentFailureWorker(appContext: AppContext) : Worker<PermanentFailurePayload>(appContext) {
    override suspend fun run(payload: PermanentFailurePayload, context: RuntimeContext): TaskResult {
        return TaskResult.Failure.Permanent(
            error = IllegalStateException("Permanent failure scenario executed")
        )
    }
}

internal class HeartbeatWorker(appContext: AppContext) : Worker<HeartbeatPayload>(appContext) {
    override suspend fun run(payload: HeartbeatPayload, context: RuntimeContext): TaskResult {
        return TaskResult.Success
    }
}
