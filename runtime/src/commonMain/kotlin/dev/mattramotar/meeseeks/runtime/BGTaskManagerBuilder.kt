package dev.mattramotar.meeseeks.runtime

import dev.mattramotar.meeseeks.runtime.impl.BGTaskManagerSingleton
import dev.mattramotar.meeseeks.runtime.impl.DynamicDataRegistration
import dev.mattramotar.meeseeks.runtime.impl.MeeseeksAppDatabase
import dev.mattramotar.meeseeks.runtime.impl.WorkerRegistry
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.serializer
import kotlin.reflect.KClass


class BGTaskManagerBuilder internal constructor(private val appContext: AppContext) {
    private var config: BGTaskManagerConfig = BGTaskManagerConfig()
    private val registrations = mutableMapOf<KClass<out DynamicData>, DynamicDataRegistration>()

    fun configuration(block: BGTaskManagerConfig.() -> BGTaskManagerConfig): BGTaskManagerBuilder {
        config = config.block()
        return this
    }

    inline fun <reified T : DynamicData> register(
        noinline factory: (appContext: AppContext) -> Worker<T>
    ): BGTaskManagerBuilder = apply {
        val type = T::class

        val serializer = serializer<T>()

        val registrations = getRegistrations()
        if (registrations.containsKey(type)) {
            throw IllegalStateException("Worker already registered for Task type: ${type.simpleName}")
        }

        val registration = DynamicDataRegistration(type, serializer, WorkerFactory(factory))
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
                polymorphic(DynamicData::class) {
                    registry.getAllRegistrations().forEach { registration ->
                        @Suppress("UNCHECKED_CAST")
                        val type = registration.type as KClass<DynamicData>
                        @Suppress("UNCHECKED_CAST")
                        val serializer = registration.serializer as KSerializer<DynamicData>
                        subclass(type, serializer)
                    }
                }
            }
        }
    }

    @PublishedApi
    internal fun getRegistrations(): Map<KClass<out DynamicData>, DynamicDataRegistration> =
        registrations

    @PublishedApi
    internal fun addRegistration(type: KClass<out DynamicData>, registration: DynamicDataRegistration) {
        registrations[type] = registration
    }
}
