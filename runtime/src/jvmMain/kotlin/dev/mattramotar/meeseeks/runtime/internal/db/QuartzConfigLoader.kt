package dev.mattramotar.meeseeks.runtime.internal.db

import java.io.InputStream
import java.util.Properties

/**
 * Resolves the Quartz configuration used by the JVM scheduler.
 *
 * Hosts may override the bundled defaults by placing a `quartz.properties`
 * file at the root of the application classpath. When no host override is
 * present, the defaults packaged in this jar are used, so
 * `Meeseeks.initialize` completes on a stock classpath.
 */
internal object QuartzConfigLoader {

    private const val HOST_RESOURCE = "quartz.properties"

    const val BUNDLED_RESOURCE: String =
        "/dev/mattramotar/meeseeks/runtime/internal/quartz/quartz.properties"

    fun load(
        contextClassLoader: ClassLoader? = Thread.currentThread().contextClassLoader,
    ): Properties {
        val stream = hostStream(contextClassLoader) ?: bundledStream()
        return Properties().also { properties -> stream.use { properties.load(it) } }
    }

    private fun hostStream(contextClassLoader: ClassLoader?): InputStream? {
        val classLoaders = listOfNotNull(
            contextClassLoader,
            QuartzConfigLoader::class.java.classLoader,
        )
        return classLoaders.firstNotNullOfOrNull { it.getResourceAsStream(HOST_RESOURCE) }
    }

    private fun bundledStream(): InputStream =
        checkNotNull(QuartzConfigLoader::class.java.getResourceAsStream(BUNDLED_RESOURCE)) {
            "Bundled Quartz configuration missing from the Meeseeks runtime jar: $BUNDLED_RESOURCE"
        }
}
