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

import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.compose.compiler)
    id("androidx.baselineprofile")
    id("org.jetbrains.dokka")
    id("android.maps.compose.PublishingConventionPlugin")
}

android {
    lint {
        sarifOutput = layout.buildDirectory.file("reports/lint-results.sarif").get().asFile
    }

    namespace = "com.google.maps.android.compose"
    compileSdk = libs.versions.androidCompileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.androidMinSdk.get().toInt()
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        buildConfig = false
        compose = true
    }

    sourceSets {
        getByName("main") {
            java.directories.add("build/generated/source/artifactId")
            kotlin.directories.add("build/generated/source/artifactId")
        }
    }

    buildTypes {
        getByName("debug") {
            enableAndroidTestCoverage = true
        }
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        freeCompilerArgs.addAll(
            "-Xexplicit-api=strict",
            "-opt-in=kotlin.RequiresOptIn"
        )
    }
}

composeCompiler {
    stabilityConfigurationFiles.set(
        listOf(rootProject.layout.projectDirectory.file("compose_compiler_stability_config.conf"))
    )

    if (findProperty("composeCompilerReports") == "true") {
        reportsDestination = layout.buildDirectory.dir("compose_compiler")
    }
    if (findProperty("composeCompilerMetrics") == "true") {
        metricsDestination = layout.buildDirectory.dir("compose_compiler")
    }
}


baselineProfile {
    // The profile is generated from maps-app, which shares the root package prefix. Keep only this
    // library's classes: exclude the other two libraries and the demo app.
    filter {
        include("com.google.maps.android.compose.**")
        exclude("com.google.maps.android.compose.clustering.**")
        exclude("com.google.maps.android.compose.wms.**")
        exclude("com.google.maps.android.compose.widgets.**")
        exclude("com.google.maps.android.compose.demo.**")
        exclude("com.google.maps.android.compose.baselineprofile.**")
    }
}

// maps-app also uses the root package, so the filter above keeps its classes too. After the
// profile is copied into src, drop every rule whose class this library does not compile. This
// also covers the demo app's nested classes and lambdas, which the filter cannot match.
tasks.matching { it.name == "copyBaselineProfileIntoSrc" }.configureEach {
    dependsOn("compileReleaseKotlin")
    val classesDir = layout.buildDirectory.dir("intermediates/built_in_kotlinc/release/compileReleaseKotlin/classes")
    val profile = layout.projectDirectory.file("src/main/generated/baselineProfiles/baseline-prof.txt").asFile
    doLast {
        val root = classesDir.get().asFile
        val kept = profile.readLines().filter { line ->
            val className = Regex("L([^;]+);").find(line)?.groupValues?.get(1) ?: return@filter true
            // D8 adds classes that kotlinc never writes: Foo$$ExternalSyntheticLambda0 for lambdas and
            // Foo$-CC for the default methods of interface Foo.
            val ownerClass = className.substringBefore("\$\$ExternalSynthetic").removeSuffix("\$-CC")
            File(root, "$className.class").exists() || File(root, "$ownerClass.class").exists()
        }
        profile.writeText(kept.joinToString(separator = "\n", postfix = "\n"))
    }
}

dependencies {
    baselineProfile(project(":baselineprofile"))
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.core)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.startup.runtime)
    implementation(libs.kotlin)
    implementation(libs.kotlinx.coroutines.android)
    api(libs.maps.utils.core)

    testImplementation(libs.test.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.truth)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.maps.testing.robolectric)
    testImplementation(libs.mockk)
    testImplementation(libs.androidx.test.compose.ui)
    testImplementation(libs.androidx.activity.compose)
    testImplementation(libs.androidx.test.core)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.test.espresso)
    androidTestImplementation(libs.androidx.test.junit.ktx)
}

// Artifact ID logic
val attributionId = "gmp_git_androidmapscompose_v$version"

val generateArtifactIdFile = tasks.register("generateArtifactIdFile") {
    val outputDir = layout.buildDirectory.dir("generated/source/artifactId")
    val packageName = "com.google.maps.android.compose.utils.meta"
    val packagePath = packageName.replace('.', '/')
    val outputFile = outputDir.get().file("$packagePath/ArtifactId.kt").asFile

    outputs.file(outputFile)

    doLast {
        outputFile.parentFile.mkdirs()
        outputFile.writeText(
            """
            package $packageName

            public object AttributionId {
                public const val VALUE: String = "$attributionId"
            }
            """.trimIndent()
        )
    }
}

tasks.named("preBuild") {
    dependsOn(generateArtifactIdFile)
}

dokka {
    dokkaSourceSets.configureEach {

    }
}