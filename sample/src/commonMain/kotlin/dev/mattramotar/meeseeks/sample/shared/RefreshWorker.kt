package dev.mattramotar.meeseeks.sample.shared

import dev.mattramotar.meeseeks.runtime.AppContext
import dev.mattramotar.meeseeks.runtime.RuntimeContext
import dev.mattramotar.meeseeks.runtime.TaskResult
import dev.mattramotar.meeseeks.runtime.Worker

class RefreshWorker(appContext: AppContext) : Worker<RefreshPayload>(appContext) {
    override suspend fun run(
        payload: RefreshPayload,
        context: RuntimeContext
    ): TaskResult {
        TODO()
    }
}