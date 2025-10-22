plugins {
    alias(libs.plugins.dokka) apply true
    alias(libs.plugins.compose.compiler) apply false
    id("com.autonomousapps.dependency-analysis") version "2.0.0"
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.maps.secrets.gradle.plugin) apply false
    alias(libs.plugins.jacoco.android) apply false
}

val projectArtifactId by extra { project: Project ->
    if (project.name in listOf("maps-compose", "maps-compose-widgets", "maps-compose-utils")) {
        project.name
    } else {
        null
    }
}

allprojects {
    group = "com.google.maps.android"
    version = "6.12.1"
    val projectArtifactId by extra { project.name }
}

tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}
