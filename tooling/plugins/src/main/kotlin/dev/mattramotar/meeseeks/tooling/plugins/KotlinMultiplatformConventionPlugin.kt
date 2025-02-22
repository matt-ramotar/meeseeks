package dev.mattramotar.meeseeks.tooling.plugins

import kotlinx.kover.gradle.plugin.dsl.KoverProjectExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.configurationcache.extensions.capitalized
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import dev.mattramotar.meeseeks.tooling.extensions.configureKotlin
import dev.mattramotar.meeseeks.tooling.extensions.libs

class KotlinMultiplatformConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        with(pluginManager) {
            apply("org.jetbrains.kotlin.multiplatform")
            apply("dev.mokkery")
            apply("org.jetbrains.kotlinx.kover")
        }

        version = libs.findVersion("meeseeks")

        extensions.configure<KotlinMultiplatformExtension> {
            applyDefaultHierarchyTemplate()

            if (pluginManager.hasPlugin("com.android.library")) {
                androidTarget()
            }

            jvm()

            iosX64()
            iosArm64()
            iosSimulatorArm64()

            js {
                browser()
            }

            targets.all {
                compilations.all {
                    compilerOptions.configure {
                        freeCompilerArgs.add("-Xexpect-actual-classes")
                    }
                }
            }

            sourceSets.commonTest.dependencies {
                val coroutinesTest = libs.findLibrary("kotlinx-coroutines-test").get()
                val kotlinTest = libs.findLibrary("kotlin-test").get()
                val turbine = libs.findLibrary("turbine").get()

                implementation(coroutinesTest)
                implementation(kotlinTest)
                implementation(turbine)
            }

            targets.withType<KotlinNativeTarget>().configureEach {
                compilations.configureEach {
                    compilerOptions.configure {
                        freeCompilerArgs.add("-Xallocator=custom")
                        freeCompilerArgs.add("-XXLanguage:+ImplicitSignedToUnsignedIntegerConversion")
                        freeCompilerArgs.add("-Xadd-light-debug=enable")

                        freeCompilerArgs.addAll(
                            "-opt-in=kotlin.RequiresOptIn",
                            "-opt-in=kotlin.time.ExperimentalTime",
                            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
                            "-opt-in=kotlinx.coroutines.FlowPreview",
                            "-opt-in=kotlinx.cinterop.ExperimentalForeignApi",
                            "-opt-in=kotlinx.cinterop.BetaInteropApi",
                        )
                    }
                }
            }

            configureKotlin()
        }

        extensions.configure<KoverProjectExtension> {
            reports {
                total {
                    xml {
                        onCheck.set(true)
                        xmlFile.set(target.layout.buildDirectory.file("reports/kover/coverage.xml"))
                    }
                }
            }
        }
    }
}

fun Project.addKspDependencyForAllTargets(dependencyNotation: Any) =
    addKspDependencyForAllTargets("", dependencyNotation)

private fun Project.addKspDependencyForAllTargets(
    configurationNameSuffix: String,
    dependencyNotation: Any,
) {
    val kmpExtension = extensions.getByType<KotlinMultiplatformExtension>()
    dependencies {
        kmpExtension.targets
            .asSequence()
            .filter { target ->
                target.platformType != KotlinPlatformType.common
            }
            .forEach { target ->
                add(
                    "ksp${target.targetName.capitalized()}$configurationNameSuffix",
                    dependencyNotation,
                )
            }
    }
}