import dev.mattramotar.meeseeks.tooling.extensions.android
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

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
    targets.withType(KotlinNativeTarget::class.java).configureEach {
        binaries.framework {
            baseName = "sample"
            isStatic = true
        }
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(projects.runtime)
                implementation(libs.kotlinx.serialization.core)
            }
        }
    }
}
