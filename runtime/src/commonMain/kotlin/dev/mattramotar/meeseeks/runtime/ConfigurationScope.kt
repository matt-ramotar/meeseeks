package dev.mattramotar.meeseeks.runtime

import dev.mattramotar.meeseeks.runtime.db.MeeseeksDatabase
import dev.mattramotar.meeseeks.runtime.internal.MeeseeksDatabaseFactory
import dev.mattramotar.meeseeks.runtime.internal.WorkerRegistration
import dev.mattramotar.meeseeks.runtime.internal.WorkerRegistry
import dev.mattramotar.meeseeks.runtime.internal.createBGTaskManager
import dev.mattramotar.meeseeks.runtime.internal.db.adapters.taskLogEntityAdapter
import dev.mattramotar.meeseeks.runtime.telemetry.Telemetry
import dev.mattramotar.meeseeks.runtime.telemetry.TelemetryEvent
import kotlinx.serialization.KSerializer
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import kotlin.reflect.KClass
import kotlin.time.Duration

public class ConfigurationScope internal constructor(private val appContext: AppContext) {
    private var config: BGTaskManagerConfig = BGTaskManagerConfig()
    private val registrations = mutableMapOf<KClass<out TaskPayload>, WorkerRegistration>()

    private val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    public fun minBackoff(duration: Duration): ConfigurationScope = apply {
        config = config.copy(minBackoff = duration)
    }

    public fun maxRetryCount(count: Int): ConfigurationScope = apply {
        config = config.copy(maxRetryCount = count)
    }

    public fun maxParallelTasks(count: Int): ConfigurationScope = apply {
        config = config.copy(maxParallelTasks = count)
    }

    public fun allowExpedited(allowed: Boolean = true): ConfigurationScope = apply {
        config = config.copy(allowExpedited = allowed)
    }

    public fun telemetry(telemetry: Telemetry): ConfigurationScope = apply {
        config = config.copy(telemetry = telemetry)
    }

    public fun telemetry(handler: (event: TelemetryEvent) -> Unit): ConfigurationScope = apply {
        config = config.copy(telemetry = Telemetry(handler))
    }

    /**
     * Enables encryption for task payloads stored in the database.
     */
    public fun payloadCipher(cipher: PayloadCipher): ConfigurationScope = apply {
        config = config.copy(payloadCipher = cipher)
    }


    /**
     * Registers a [Worker] for the given [TaskPayload] type.
     *
     * The stable identifier for serialization is automatically derived from [SerialDescriptor.serialName] (`descriptor.serialName`).
     * By default, this is the fully qualified class name (e.g., `com.example.model.ExamplePayload`).
     *
     * To customize the identifier, use the [kotlinx.serialization.SerialName] annotation on your [TaskPayload] class:
     *
     * ```kotlin
     * @Serializable
     * @SerialName("custom")
     * data class ExamplePayload(...) : TaskPayload
     * ```
     *
     * @param factory Factory function to create the [Worker] instance.
     */
    @OptIn(ExperimentalSerializationApi::class)
    public inline fun <reified T : TaskPayload> register(
        noinline factory: (appContext: AppContext) -> Worker<T>
    ): ConfigurationScope = register(
        type = T::class,
        serializer = serializer<T>(),
        factory = factory,
    )

    @OptIn(ExperimentalSerializationApi::class)
    @PublishedApi
    internal fun <T : TaskPayload> register(
        type: KClass<T>,
        serializer: KSerializer<T>,
        factory: (appContext: AppContext) -> Worker<T>,
    ): ConfigurationScope = apply {
        val stableId = serializer.descriptor.serialName

        if (registrations.values.any { it.typeId == stableId }) {
            throw IllegalStateException("Duplicate stableId registered: $stableId.")
        }

        if (registrations.containsKey(type)) {
            throw IllegalStateException("Worker already registered for Task type: ${type.simpleName}")
        }

        val registration = WorkerRegistration(type, stableId, serializer, WorkerFactory(factory))
        registrations[type] = registration
    }

    internal fun build(): BGTaskManager {
        val registry = WorkerRegistry(registrations, json, config.payloadCipher)
        val database = createDatabaseInstance(appContext, json)
        return createBGTaskManager(appContext, database, registry, json, config)
    }

    private fun createDatabaseInstance(context: AppContext, json: Json): MeeseeksDatabase {
        val factory = MeeseeksDatabaseFactory()
        return factory.create(
            context = context,
            taskLogAdapter = taskLogEntityAdapter(json),
        )
    }
}
