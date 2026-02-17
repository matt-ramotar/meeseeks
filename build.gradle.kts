
import org.gradle.api.GradleException
import java.io.File
import java.util.Properties

buildscript {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.compose) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.buildkonfig) apply false
    alias(libs.plugins.kotlin.plugin.parcelize) apply false
    alias(libs.plugins.maven.publish) apply false
    alias(libs.plugins.binary.compatibility.validator) apply false
}

allprojects {
    configurations.configureEach {
        if (isCanBeResolved) {
            resolutionStrategy.activateDependencyLocking()
        }
    }
}

tasks.register("preflight") {
    group = "verification"
    description = "Checks local Android SDK prerequisites and optional CHROME_BIN."

    doLast {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        val localPropertiesFile = rootProject.file("local.properties")
        val localProperties = Properties()
        val sdkDirFromLocalProperties = if (localPropertiesFile.exists()) {
            localPropertiesFile.inputStream().use(localProperties::load)
            localProperties.getProperty("sdk.dir")?.trim()?.takeIf { it.isNotEmpty() }
        } else {
            null
        }

        val androidHome = System.getenv("ANDROID_HOME")?.trim()?.takeIf { it.isNotEmpty() }
        val androidSdkRoot = System.getenv("ANDROID_SDK_ROOT")?.trim()?.takeIf { it.isNotEmpty() }

        fun existingDirectory(path: String?): String? {
            val candidate = path?.trim()?.takeIf { it.isNotEmpty() } ?: return null
            val directory = File(candidate)
            return if (directory.exists() && directory.isDirectory) directory.absolutePath else null
        }

        val resolvedAndroidSdk = existingDirectory(androidHome)
            ?: existingDirectory(androidSdkRoot)
            ?: existingDirectory(sdkDirFromLocalProperties)

        if (resolvedAndroidSdk == null) {
            errors += """
                Android SDK not found.
                Provide one of:
                  - ANDROID_HOME
                  - ANDROID_SDK_ROOT
                  - local.properties with sdk.dir=/absolute/path/to/Android/sdk
            """.trimIndent()
        } else {
            val sdkRoot = File(resolvedAndroidSdk)
            val requiredDirs = listOf("platform-tools", "licenses")
            val missingRequiredDirs = requiredDirs.filterNot { name ->
                File(sdkRoot, name).exists()
            }
            val hasBuildToolsOrPlatforms = listOf("build-tools", "platforms").any { name ->
                File(sdkRoot, name).exists()
            }

            if (missingRequiredDirs.isNotEmpty() || !hasBuildToolsOrPlatforms) {
                val missingParts = buildList {
                    if (missingRequiredDirs.isNotEmpty()) {
                        add("missing directories: ${missingRequiredDirs.joinToString(", ")}")
                    }
                    if (!hasBuildToolsOrPlatforms) {
                        add("missing at least one of: build-tools or platforms")
                    }
                }
                errors += "Android SDK path is set but incomplete ($resolvedAndroidSdk): ${missingParts.joinToString("; ")}"
            }
        }

        val chromeBin = System.getenv("CHROME_BIN")?.trim()?.takeIf { it.isNotEmpty() }
        if (chromeBin == null) {
            warnings += """
                CHROME_BIN is not set.
                JS tests can still pass when Gradle auto-discovers Chrome/Chromium.
                Set CHROME_BIN only if browser discovery fails while running :runtime:jsTest.
                Examples:
                  - macOS: export CHROME_BIN="/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"
                  - Linux: export CHROME_BIN=/usr/bin/google-chrome
            """.trimIndent()
        } else {
            val chromeBinary = File(chromeBin)
            if (!chromeBinary.exists()) {
                errors += "CHROME_BIN points to a missing file: $chromeBin"
            } else if (!chromeBinary.isFile) {
                errors += "CHROME_BIN must point to a browser binary file, but found: $chromeBin"
            } else if (!chromeBinary.canExecute()) {
                errors += "CHROME_BIN is not executable: $chromeBin"
            }
        }

        if (errors.isNotEmpty()) {
            throw GradleException(
                buildString {
                    appendLine("Preflight failed. Fix the following prerequisite issues:")
                    errors.forEachIndexed { index, error ->
                        appendLine()
                        appendLine("${index + 1}. $error")
                    }
                }
            )
        }

        warnings.forEach { warning ->
            logger.warn(warning)
        }
        logger.lifecycle("Preflight passed: Android SDK is valid for build prerequisites.")
    }
}
