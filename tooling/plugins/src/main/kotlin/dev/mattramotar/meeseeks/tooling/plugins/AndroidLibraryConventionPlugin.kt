package dev.mattramotar.meeseeks.tooling.plugins

import com.android.build.gradle.LibraryExtension
import dev.mattramotar.meeseeks.tooling.extensions.Versions
import dev.mattramotar.meeseeks.tooling.extensions.configureAndroid
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("com.android.library")
            }

            extensions.configure<LibraryExtension> {
                compileOptions {
                    sourceCompatibility = JavaVersion.VERSION_17
                    targetCompatibility = JavaVersion.VERSION_17
                }

                configureAndroid()
                defaultConfig.targetSdk = Versions.TARGET_SDK
            }
        }
    }
}
