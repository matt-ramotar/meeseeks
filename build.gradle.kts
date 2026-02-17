
import org.gradle.api.GradleException
import java.io.File
import java.util.Properties

buildscript {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        maven("https://oss.sonatype.org/content/repositories/snapshots")
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
    description = "Checks local Android SDK and CHROME_BIN prerequisites."

    doLast {
        val errors = mutableListOf<String>()

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

        fun existingPath(path: String?): String? = path?.takeIf { File(it).exists() }

        val resolvedAndroidSdk = existingPath(androidHome)
            ?: existingPath(androidSdkRoot)
            ?: existingPath(sdkDirFromLocalProperties)

        if (resolvedAndroidSdk == null) {
            errors += """
                Android SDK not found.
                Provide one of:
                  - ANDROID_HOME
                  - ANDROID_SDK_ROOT
                  - local.properties with sdk.dir=/absolute/path/to/Android/sdk
            """.trimIndent()
        }

        val chromeBin = System.getenv("CHROME_BIN")?.trim()?.takeIf { it.isNotEmpty() }
        if (chromeBin == null) {
            errors += """
                CHROME_BIN is not set.
                Set CHROME_BIN to your Chrome/Chromium binary before running :runtime:jsTest.
                Examples:
                  - macOS: export CHROME_BIN="/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"
                  - Linux: export CHROME_BIN=/usr/bin/google-chrome
            """.trimIndent()
        } else if (!File(chromeBin).exists()) {
            errors += "CHROME_BIN points to a missing file: $chromeBin"
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

        logger.lifecycle("Preflight passed: Android SDK and CHROME_BIN are configured.")
    }
}

