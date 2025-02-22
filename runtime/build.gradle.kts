@file:Suppress("UnstableApiUsage")

import com.vanniktech.maven.publish.SonatypeHost.Companion.CENTRAL_PORTAL
import dev.mattramotar.meeseeks.tooling.extensions.android

plugins {
    alias(libs.plugins.compose)
    alias(libs.plugins.compose.compiler)
    id("plugin.meeseeks.android.library")
    id("plugin.meeseeks.kotlin.multiplatform")
    alias(libs.plugins.maven.publish)
}

android {
    namespace = "dev.mattramotar.meeseeks.runtime"

    testOptions {
        unitTests {
            isReturnDefaultValues = true
        }
    }
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(compose.runtime)
                api(libs.kotlinx.coroutines.core)
                implementation(libs.molecule.runtime)
            }
        }
    }
}

mavenPublishing {
    publishToMavenCentral(CENTRAL_PORTAL, automaticRelease = true)
    signAllPublications()
}