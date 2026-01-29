// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath(libs.android.gradle.plugin)
        classpath(libs.maps.secrets.plugin)
        classpath(libs.kotlin.gradle.plugin)
        classpath(libs.dokka.plugin)
        classpath(libs.jacoco.android.plugin)
    }
}

plugins {
    id("org.jetbrains.dokka") version "2.1.0"
    alias(libs.plugins.compose.compiler) apply false
    id("com.autonomousapps.dependency-analysis") version "3.4.1"
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false

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
    // {x-release-please-start-version}
    version = "8.0.1"
    // {x-release-please-end}
}

tasks.register<Exec>("installAndLaunch") {
    description = "Installs and launches the demo app."
    group = "install"
    dependsOn(":maps-app:installDebug")
    commandLine("adb", "shell", "am", "start", "-n", "com.google.maps.android.compose/.MainActivity")
}