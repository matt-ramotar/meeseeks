package dev.mattramotar.meeseeks.runtime.internal

import dev.mattramotar.meeseeks.runtime.AppContext
import dev.mattramotar.meeseeks.runtime.TaskPayload
import dev.mattramotar.meeseeks.runtime.Worker
import dev.mattramotar.meeseeks.runtime.WorkerFactory
import kotlin.reflect.KClass

internal class WorkerRegistry(private val registrations: Map<KClass<out TaskPayload>, WorkerRegistration>) {

    fun <T : TaskPayload> getFactory(payloadClass: KClass<T>): WorkerFactory<T> {
        val registration = registrations[payloadClass]

        @Suppress("UNCHECKED_CAST")
        return registration?.factory as? WorkerFactory<T>
            ?: throw IllegalArgumentException("No Worker registered for Payload type: ${payloadClass.simpleName}. Ensure it was added in BGTaskManagerBuilder.")
    }

    inline fun <reified T : TaskPayload> getWorker(appContext: AppContext): Worker<T> {
        val factory = getFactory(T::class)
        return factory.create(appContext)
    }

    internal fun getAllRegistrations(): Collection<WorkerRegistration> = registrations.values
}