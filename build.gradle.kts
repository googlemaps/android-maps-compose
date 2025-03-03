// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    val kotlinVersion by extra(libs.versions.kotlin.get())
    val androidxTestVersion by extra(libs.versions.androidxtest.get())
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
    alias(libs.plugins.dokka) apply true
    alias(libs.plugins.compose.compiler) apply false
    id("com.autonomousapps.dependency-analysis") version "2.0.0"
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
    version = "6.5.0"
    val projectArtifactId by extra { project.name }
}

tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}
