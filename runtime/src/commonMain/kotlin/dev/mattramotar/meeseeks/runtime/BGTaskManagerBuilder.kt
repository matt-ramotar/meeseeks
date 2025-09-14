package dev.mattramotar.meeseeks.runtime

import dev.mattramotar.meeseeks.runtime.internal.BGTaskManagerSingleton
import dev.mattramotar.meeseeks.runtime.internal.WorkerRegistration
import dev.mattramotar.meeseeks.runtime.internal.MeeseeksAppDatabase
import dev.mattramotar.meeseeks.runtime.internal.WorkerRegistry
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.serializer
import kotlin.reflect.KClass


class BGTaskManagerBuilder internal constructor(private val appContext: AppContext) {
    private var config: BGTaskManagerConfig = BGTaskManagerConfig()
    private val registrations = mutableMapOf<KClass<out TaskPayload>, WorkerRegistration>()

    fun configuration(block: BGTaskManagerConfig.() -> BGTaskManagerConfig): BGTaskManagerBuilder {
        config = config.block()
        return this
    }

    inline fun <reified T : TaskPayload> register(
        noinline factory: (appContext: AppContext) -> Worker<T>
    ): BGTaskManagerBuilder = apply {
        val type = T::class

        val serializer = serializer<T>()

        val registrations = getRegistrations()
        if (registrations.containsKey(type)) {
            throw IllegalStateException("Worker already registered for Task type: ${type.simpleName}")
        }

        val registration = WorkerRegistration(type, serializer, WorkerFactory(factory))
        addRegistration(type, registration)
    }

    fun build(): BGTaskManager {
        val registry = WorkerRegistry(getRegistrations())
        MeeseeksAppDatabase.init(appContext)
        val manager = BGTaskManagerSingleton.getOrCreate(appContext, registry, config)
        initializePlatformDependencies(appContext, manager, registry, config)
        configureJson(registry)
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
