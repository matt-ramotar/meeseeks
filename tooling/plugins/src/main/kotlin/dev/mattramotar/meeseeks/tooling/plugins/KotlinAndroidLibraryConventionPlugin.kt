package dev.mattramotar.meeseeks.tooling.plugins

import com.android.build.gradle.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import dev.mattramotar.meeseeks.tooling.extensions.Versions
import dev.mattramotar.meeseeks.tooling.extensions.configureKotlin

class KotlinAndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("com.android.library")
            }

            configureKotlin()

            extensions.configure<LibraryExtension> {
                compileSdk = Versions.COMPILE_SDK
            }
        }
    }
}