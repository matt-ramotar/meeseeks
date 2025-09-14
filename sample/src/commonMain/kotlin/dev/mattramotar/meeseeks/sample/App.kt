package dev.mattramotar.meeseeks.sample

import dev.mattramotar.meeseeks.runtime.AppContext
import dev.mattramotar.meeseeks.runtime.Meeseeks
import dev.mattramotar.meeseeks.sample.shared.CustomTelemetry
import dev.mattramotar.meeseeks.sample.shared.RefreshPayload
import dev.mattramotar.meeseeks.sample.shared.RefreshWorker
import dev.mattramotar.meeseeks.sample.shared.SyncPayload
import dev.mattramotar.meeseeks.sample.shared.SyncWorker
import kotlin.time.Duration.Companion.seconds

class App(
    private val appContext: AppContext,
    private val telemetry: CustomTelemetry,
    private val syncWorker: SyncWorker,
    private val refreshWorker: RefreshWorker
) {

    fun initializeMeeseeks() {
        Meeseeks.initialize(appContext) {
            minBackoff(20.seconds)
            maxRetryCount(3)
            maxParallelTasks(5)
            allowExpedited()
            telemetry(telemetry)
            register<SyncPayload> { syncWorker }
            register<RefreshPayload> { refreshWorker }
        }
    }
}


