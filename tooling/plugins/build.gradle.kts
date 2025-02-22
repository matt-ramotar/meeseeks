plugins {
    `kotlin-dsl`
}

group = "dev.mattramotar.meeseeks.tooling"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17

    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

dependencies {
    compileOnly(libs.android.gradle.plugin)
    compileOnly(libs.kotlin.gradle.plugin)
    compileOnly(libs.dokka.gradle.plugin)
    compileOnly(libs.maven.publish.plugin)
    implementation(libs.mokkery.gradle)
    implementation(libs.kover.gradle.plugin)
}

gradlePlugin {
    plugins {

        register("androidLibraryPlugin") {
            id = "plugin.meeseeks.android.library"
            implementationClass = "dev.mattramotar.meeseeks.tooling.plugins.AndroidLibraryConventionPlugin"
        }

        register("kotlinAndroidLibraryPlugin") {
            id = "plugin.meeseeks.kotlin.android.library"
            implementationClass = "dev.mattramotar.meeseeks.tooling.plugins.KotlinAndroidLibraryConventionPlugin"
        }

        register("kotlinMultiplatformPlugin") {
            id = "plugin.meeseeks.kotlin.multiplatform"
            implementationClass = "dev.mattramotar.meeseeks.tooling.plugins.KotlinMultiplatformConventionPlugin"
        }
    }
}