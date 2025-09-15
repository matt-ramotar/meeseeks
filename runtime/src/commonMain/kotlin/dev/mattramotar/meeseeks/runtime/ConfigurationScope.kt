package dev.mattramotar.meeseeks.runtime

import dev.mattramotar.meeseeks.runtime.internal.BGTaskManagerSingleton
import dev.mattramotar.meeseeks.runtime.internal.MeeseeksAppDatabase
import dev.mattramotar.meeseeks.runtime.internal.WorkerRegistration
import dev.mattramotar.meeseeks.runtime.internal.WorkerRegistry
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.serializer
import kotlin.reflect.KClass
import kotlin.time.Duration


class ConfigurationScope internal constructor(private val appContext: AppContext) {
    private var config: BGTaskManagerConfig = BGTaskManagerConfig()
    private val registrations = mutableMapOf<KClass<out TaskPayload>, WorkerRegistration>()


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


    fun telemetry(telemetry: TaskTelemetry): ConfigurationScope = apply {
        config = config.copy(telemetry = telemetry)
    }

    fun telemetry(handler: (event: TaskTelemetryEvent) -> Unit): ConfigurationScope = apply {
        config = config.copy(telemetry = TaskTelemetry(handler))
    }


    inline fun <reified T : TaskPayload> register(
        noinline factory: (appContext: AppContext) -> Worker<T>
    ): ConfigurationScope = apply {
        val type = T::class

        val serializer = serializer<T>()

        val registrations = getRegistrations()
        if (registrations.containsKey(type)) {
            throw IllegalStateException("Worker already registered for Task type: ${type.simpleName}")
        }

        val registration = WorkerRegistration(type, serializer, WorkerFactory(factory))
        addRegistration(type, registration)
    }

    internal fun build(): BGTaskManager {
        val registry = WorkerRegistry(getRegistrations())
        val json = configureJson(registry)
        MeeseeksAppDatabase.init(appContext, json)
        val manager = BGTaskManagerSingleton.getOrCreate(appContext, registry, json, config)
        initializePlatformDependencies(appContext, manager, registry, json, config)
        return manager
    }


    private fun configureJson(registry: WorkerRegistry): Json {
        return Json {
            classDiscriminator = "__type"
            ignoreUnknownKeys = true
            serializersModule = SerializersModule {
                polymorphic(TaskPayload::class) {
                    registry.getAllRegistrations().forEach { registration ->
                        @Suppress("UNCHECKED_CAST")
                        val type = registration.type as KClass<TaskPayload>

                        @Suppress("UNCHECKED_CAST")
                        val serializer = registration.serializer as KSerializer<TaskPayload>
                        subclass(type, serializer)
                    }
                }
            }
        }
    }

    @PublishedApi
    internal fun getRegistrations(): Map<KClass<out TaskPayload>, WorkerRegistration> =
        registrations

    @PublishedApi
    internal fun addRegistration(type: KClass<out TaskPayload>, registration: WorkerRegistration) {
        registrations[type] = registration
    }
}
