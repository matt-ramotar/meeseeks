package dev.mattramotar.meeseeks.runtime.impl

import dev.mattramotar.meeseeks.runtime.AppContext
import dev.mattramotar.meeseeks.runtime.DynamicData
import dev.mattramotar.meeseeks.runtime.Worker
import dev.mattramotar.meeseeks.runtime.WorkerFactory
import kotlin.reflect.KClass

internal class WorkerRegistry(private val registrations: Map<KClass<out DynamicData>, DynamicDataRegistration>) {

    fun <T : DynamicData> getFactory(dynamicDataKClass: KClass<T>): WorkerFactory<T> {
        val registration = registrations[dynamicDataKClass]

        @Suppress("UNCHECKED_CAST")
        return registration?.factory as? WorkerFactory<T>
            ?: throw IllegalArgumentException("No Worker registered for DynamicData type: ${dynamicDataKClass.simpleName}. Ensure it was added in BGTaskManagerBuilder.")
    }

    inline fun <reified T : DynamicData> getWorker(appContext: AppContext): Worker<T> {
        val factory = getFactory(T::class)
        return factory.create(appContext)
    }

    internal fun getAllRegistrations(): Collection<DynamicDataRegistration> = registrations.values
}