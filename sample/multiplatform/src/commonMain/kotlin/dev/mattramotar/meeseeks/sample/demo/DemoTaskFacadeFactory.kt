package dev.mattramotar.meeseeks.sample.demo

import dev.mattramotar.meeseeks.runtime.AppContext
import dev.mattramotar.meeseeks.runtime.Meeseeks
import kotlin.time.Duration.Companion.seconds

public data class DemoSampleConfig(
    public val encryptionEnabled: Boolean = false,
    public val maxTelemetryEvents: Int = 400,
)

public object DemoTaskFacadeFactory {
    public fun create(appContext: AppContext, config: DemoSampleConfig = DemoSampleConfig()): DemoTaskFacade {
        val demoTelemetry = DemoTelemetry(maxEventRetention = config.maxTelemetryEvents)

        val taskManager = Meeseeks.initialize(appContext) {
            minBackoff(5.seconds)
            maxRetryCount(5)
            maxParallelTasks(4)
            allowExpedited()
            telemetry(demoTelemetry)

            if (config.encryptionEnabled) {
                payloadCipher(DemoPayloadCipher())
            }

            register<SuccessPayload> { SuccessWorker(appContext) }
            register<RetryThenSuccessPayload> { RetryThenSuccessWorker(appContext) }
            register<PermanentFailurePayload> { PermanentFailureWorker(appContext) }
            register<HeartbeatPayload> { HeartbeatWorker(appContext) }
        }

        return DemoTaskFacade(taskManager = taskManager, telemetry = demoTelemetry)
    }
}
