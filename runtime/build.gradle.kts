@file:Suppress("UnstableApiUsage")

import dev.mattramotar.meeseeks.tooling.extensions.android

plugins {
    id("plugin.meeseeks.android.library")
    id("plugin.meeseeks.kotlin.multiplatform")
    alias(libs.plugins.maven.publish)
    alias(libs.plugins.sqldelight)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.binary.compatibility.validator)
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
    explicitApi()

    sourceSets {
        commonMain {
            dependencies {
                api(libs.kotlinx.coroutines.core)
                api(libs.kotlinx.serialization.core)
                api(libs.kotlinx.serialization.json)
                implementation(libs.kotlin.reflect)
                implementation(libs.coroutines.extensions)
            }
        }

        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.kotlinx.coroutines.test)
            }
        }

        androidMain {
            dependencies {
                api(libs.androidx.work.runtime.ktx)
                implementation(libs.android.driver)
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
                implementation(libs.quartz)
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

apiValidation {
    @OptIn(kotlinx.validation.ExperimentalBCVApi::class)
    klib {
        enabled = true
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
    publishToMavenCentral(automaticRelease = true)
    signAllPublications()
}
