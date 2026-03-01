plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    jvm()

    sourceSets {
        jvmMain {
            dependencies {
                implementation(projects.sample.multiplatform)
                implementation(projects.runtime)
                implementation("org.jetbrains.compose.desktop:desktop-jvm:1.7.0")
                implementation("org.jetbrains.compose.material3:material3:1.7.0")
            }
        }
    }
}
