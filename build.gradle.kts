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
    }
}

plugins {
    alias(libs.plugins.dokka)
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.dependency.analysis)
    alias(libs.plugins.versions)
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.spotless)
}

spotless {
    kotlin {
        target("**/*.kt")
        targetExclude("**/build/**")
        ktlint().editorConfigOverride(
            mapOf(
                "indent_size" to "4",
                "ktlint_function_naming_ignore_when_annotated_with" to "Composable",
                "max_line_length" to "120",
                // Keep existing names such as `timeout2` and `private val TAG` (no renames in a formatting-only change).
                "ktlint_standard_property-naming" to "disabled",
                // Keep existing file names such as `MapsInLazyColumnTest.kt` (class is `MapsInLazyColumnTests`).
                "ktlint_standard_filename" to "disabled",
            ),
        )
        trimTrailingWhitespace()
        endWithNewline()
    }
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
    version = "9.0.1"
    // {x-release-please-end}
}

tasks.register<Exec>("installAndLaunch") {
    description = "Installs and launches the demo app."
    group = "install"
    dependsOn(":maps-app:installDebug")
    commandLine("adb", "shell", "am", "start", "-n", "com.google.maps.android.compose/.MainActivity")
}

configure<com.diffplug.gradle.spotless.SpotlessExtension> {
    kotlin {
        target("snippets/**/*.kt")
        targetExclude("**/build/**/*.kt")
        ktfmt("0.46").googleStyle()
    }
}