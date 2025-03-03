@file:Suppress("UnstableApiUsage")

import com.vanniktech.maven.publish.SonatypeHost.Companion.CENTRAL_PORTAL
import dev.mattramotar.meeseeks.tooling.extensions.android

plugins {
    alias(libs.plugins.compose)
    alias(libs.plugins.compose.compiler)
    id("plugin.meeseeks.android.library")
    id("plugin.meeseeks.kotlin.multiplatform")
    alias(libs.plugins.maven.publish)
    alias(libs.plugins.sqldelight)
    alias(libs.plugins.kotlin.serialization)
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
                implementation(libs.kotlinx.serialization.core)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.kotlin.reflect)
                implementation(libs.kotlinx.datetime)
            }
        }

        androidMain {
            dependencies {
                implementation(libs.android.driver)
                implementation(libs.androidx.work.runtime.ktx)
            }
        }

        nativeMain {
            dependencies {
                implementation(libs.native.driver)
            }
        }

        jvmMain {
            dependencies {
                implementation(libs.sqlite.driver)
            }
        }

        jsMain {
            dependencies {
                implementation(npm("@cashapp/sqldelight-sqljs-worker", "2.0.2"))
                implementation(npm("sql.js", "1.6.2"))
                implementation(devNpm("copy-webpack-plugin", "9.1.0"))
                implementation(libs.web.worker.driver)
            }
        }
    }
}

sqldelight {
    databases {
        create("MeeseeksDatabase") {
            packageName.set("dev.mattramotar.meeseeks.runtime.db")
        }
    }
}

mavenPublishing {
    publishToMavenCentral(CENTRAL_PORTAL, automaticRelease = true)
    signAllPublications()
}