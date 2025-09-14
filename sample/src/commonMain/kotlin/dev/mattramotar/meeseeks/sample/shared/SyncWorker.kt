package dev.mattramotar.meeseeks.sample.shared

import dev.mattramotar.meeseeks.runtime.AppContext
import dev.mattramotar.meeseeks.runtime.RuntimeContext
import dev.mattramotar.meeseeks.runtime.TaskResult
import dev.mattramotar.meeseeks.runtime.Worker

class SyncWorker(appContext: AppContext) : Worker<SyncPayload>(appContext) {
    override suspend fun run(
        payload: SyncPayload,
        context: RuntimeContext
    ): TaskResult {
        TODO()
    }
}