/*
 * Copyright 2026 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
    alias(libs.plugins.dokka)
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.dependency.analysis)
    alias(libs.plugins.versions)
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
    version = "8.6.0"
    // {x-release-please-end}
}

tasks.register<Exec>("installAndLaunch") {
    description = "Installs and launches the demo app."
    group = "install"
    dependsOn(":maps-app:installDebug")
    commandLine("adb", "shell", "am", "start", "-n", "com.google.maps.android.compose/.MainActivity")
}

// What it does:
// Scans all Kotlin source files across the project (excluding build directories) and replaces any
// import references to "com.google.maps.android.ktx" with the canonical "com.google.maps.android" package.
//
// Why:
// Starting in android-maps-utils v6.0.0-rc01, all Kotlin extensions, DSL builders, and coroutine
// wrappers previously in android-maps-ktx were consolidated into android-maps-utils under the
// canonical package "com.google.maps.android". The old "*.ktx.*" symbols are deprecated typealiases
// and forwarding bridges. Migrating the imports removes compiler deprecation warnings and ensures
// the Compose library depends directly on canonical core utilities.
tasks.register("migrateKtxImports") {
    description = "Updates Kotlin source files to drop the 'ktx' package segment from Google Maps Android imports."
    group = "refactoring"
    doLast {
        val count = java.util.concurrent.atomic.AtomicInteger(0)
        fileTree(projectDir) {
            include("**/*.kt")
            exclude("**/build/**")
        }.forEach { file ->
            val content = file.readText()
            if (content.contains("com.google.maps.android.ktx")) {
                val updated = content
                    .replace("com.google.maps.android.ktx.utils.", "com.google.maps.android.")
                    .replace("com.google.maps.android.ktx.", "com.google.maps.android.")
                if (updated != content) {
                    file.writeText(updated)
                    count.incrementAndGet()
                    println("Updated imports in: ${file.relativeTo(projectDir)}")
                }
            }
        }
        println("Migration complete. Updated ${count.get()} files.")
    }
}