import dev.mattramotar.meeseeks.tooling.extensions.android

plugins {
    id("plugin.meeseeks.android.library")
    id("plugin.meeseeks.kotlin.multiplatform")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "dev.mattramotar.meeseeks.sample"

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
                implementation(projects.runtime)
                implementation(libs.kotlinx.serialization.core)
            }
        }
    }
}