import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedArtifactResult
import org.gradle.jvm.JvmLibrary
import org.gradle.language.base.artifact.SourcesArtifact

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

configurations.configureEach {
    if (isCanBeResolved) {
        resolutionStrategy.activateDependencyLocking()
    }
}

dependencies {
    compileOnly(libs.android.gradle.plugin)
    compileOnly(libs.kotlin.gradle.plugin)
    compileOnly(libs.dokka.gradle.plugin)
    compileOnly(libs.maven.publish.plugin)
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

tasks.register("resolveVerificationSources") {
    group = "verification"
    description = "Resolves source artifacts for plugin classpaths used by IDE sync."

    doLast {
        val projectModuleComponentIds = configurations
            .filter { it.isCanBeResolved }
            .flatMap { configuration ->
                configuration.incoming.resolutionResult.allComponents
                    .mapNotNull { it.id as? ModuleComponentIdentifier }
            }
            .toSet()

        val buildscriptModuleComponentIds = buildscript.configurations
            .getByName("classpath")
            .incoming
            .resolutionResult
            .allComponents
            .mapNotNull { it.id as? ModuleComponentIdentifier }
            .toSet()

        val moduleComponentIds = (projectModuleComponentIds + buildscriptModuleComponentIds).toSet()

        if (moduleComponentIds.isEmpty()) {
            return@doLast
        }

        val resolution = dependencies.createArtifactResolutionQuery()
            .forComponents(moduleComponentIds)
            .withArtifacts(JvmLibrary::class.java, SourcesArtifact::class.java)
            .execute()

        resolution.resolvedComponents.forEach { component ->
            component.getArtifacts(SourcesArtifact::class.java).forEach { artifact ->
                if (artifact is ResolvedArtifactResult) {
                    artifact.file
                }
            }
        }

        // Kotlin Gradle plugin publishes Gradle-targeted source artifacts with a classifier
        // (for example, gradle88-sources). Resolve them explicitly so verification metadata
        // contains the exact artifacts requested by IDE sync.
        val kotlinVersion = libs.versions.kotlin.get()
        val explicitGradleSources = configurations.detachedConfiguration(
            dependencies.create("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion:gradle88-sources@jar"),
            dependencies.create("org.jetbrains.kotlin:kotlin-gradle-plugin-api:$kotlinVersion:gradle88-sources@jar"),
            dependencies.create("org.jetbrains.kotlin:fus-statistics-gradle-plugin:$kotlinVersion:gradle88-sources@jar"),
        ).apply {
            isTransitive = false
        }

        explicitGradleSources.resolve()
    }
}
