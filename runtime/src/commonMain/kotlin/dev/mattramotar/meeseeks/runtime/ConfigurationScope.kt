package dev.mattramotar.meeseeks.runtime

import dev.mattramotar.meeseeks.runtime.internal.BGTaskManagerSingleton
import dev.mattramotar.meeseeks.runtime.internal.MeeseeksDatabaseSingleton
import dev.mattramotar.meeseeks.runtime.internal.WorkerRegistration
import dev.mattramotar.meeseeks.runtime.internal.WorkerRegistry
import dev.mattramotar.meeseeks.runtime.telemetry.Telemetry
import dev.mattramotar.meeseeks.runtime.telemetry.TelemetryEvent
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import kotlin.reflect.KClass
import kotlin.time.Duration


class ConfigurationScope internal constructor(private val appContext: AppContext) {
    private var config: BGTaskManagerConfig = BGTaskManagerConfig()
    private val registrations = mutableMapOf<KClass<out TaskPayload>, WorkerRegistration>()

    private val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun minBackoff(duration: Duration): ConfigurationScope = apply {
        config = config.copy(minBackoff = duration)
    }

    fun maxRetryCount(count: Int): ConfigurationScope = apply {
        config = config.copy(maxRetryCount = count)
    }

    fun maxParallelTasks(count: Int): ConfigurationScope = apply {
        config = config.copy(maxParallelTasks = count)
    }

    fun allowExpedited(allowed: Boolean = true): ConfigurationScope = apply {
        config = config.copy(allowExpedited = allowed)
    }


    fun telemetry(telemetry: Telemetry): ConfigurationScope = apply {
        config = config.copy(telemetry = telemetry)
    }

    fun telemetry(handler: (event: TelemetryEvent) -> Unit): ConfigurationScope = apply {
        config = config.copy(telemetry = Telemetry(handler))
    }


    /**
     * @param stableId Stable [TaskPayload] type identifier. Used for serialization.
     */
    inline fun <reified T : TaskPayload> register(
        stableId: String,
        noinline factory: (appContext: AppContext) -> Worker<T>
    ): ConfigurationScope = apply {
        val type = T::class
        val serializer = serializer<T>()

        if (getRegistrations().values.any { it.typeId == stableId}) {
            throw IllegalStateException("Duplicate stableId registered: $stableId.")
        }

        val registrations = getRegistrations()
        if (registrations.containsKey(type)) {
            throw IllegalStateException("Worker already registered for Task type: ${type.simpleName}")
        }

        val registration = WorkerRegistration(type, stableId, serializer, WorkerFactory(factory))
        addRegistration(type, registration)
    }

    internal fun build(): BGTaskManager {
        val registry = WorkerRegistry(getRegistrations(), json)
        MeeseeksDatabaseSingleton.init(appContext, json)
        val manager = BGTaskManagerSingleton.getOrCreate(appContext, registry, json, config)
        initializePlatformDependencies(appContext, manager, registry, json, config)
        return manager
    }


    @PublishedApi
    internal fun getRegistrations(): Map<KClass<out TaskPayload>, WorkerRegistration> =
        registrations

    @PublishedApi
    internal fun addRegistration(type: KClass<out TaskPayload>, registration: WorkerRegistration) {
        registrations[type] = registration
    }
}
