package dev.mattramotar.meeseeks.runtime.internal.db

import java.net.URLClassLoader
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class QuartzConfigLoaderTest {

    @Test
    fun hostQuartzPropertiesOnClasspathRootWinOverBundledDefaults() {
        val hostDir = Files.createTempDirectory("meeseeks-host-config")
        val hostFile = hostDir.resolve("quartz.properties")
        hostFile.writeText("org.quartz.scheduler.instanceName = HostScheduler\n")

        try {
            val properties = classLoaderFor(hostDir).use { hostClassLoader ->
                QuartzConfigLoader.load(contextClassLoader = hostClassLoader)
            }

            assertEquals(
                "HostScheduler",
                properties.getProperty("org.quartz.scheduler.instanceName"),
            )
        } finally {
            Files.deleteIfExists(hostFile)
            Files.deleteIfExists(hostDir)
        }
    }

    @Test
    fun bundledDefaultsLoadWhenNoHostOverrideExists() {
        val emptyDir = Files.createTempDirectory("meeseeks-no-host-config")

        try {
            val properties = classLoaderFor(emptyDir).use { hostClassLoader ->
                QuartzConfigLoader.load(contextClassLoader = hostClassLoader)
            }

            assertEquals(
                "MeeseeksScheduler",
                properties.getProperty("org.quartz.scheduler.instanceName"),
            )
            // The factory derives the Quartz JDBC URL from these properties;
            // the bundled defaults must satisfy that contract out of the box.
            val jdbcUrl = QuartzProps.jdbcUrlFromQuartzProps(properties)
            assertTrue(jdbcUrl.startsWith("jdbc:sqlite:"), "Unexpected JDBC URL: $jdbcUrl")
        } finally {
            Files.deleteIfExists(emptyDir)
        }
    }

    private fun classLoaderFor(directory: Path): URLClassLoader =
        URLClassLoader(arrayOf(directory.toUri().toURL()), null)
}
