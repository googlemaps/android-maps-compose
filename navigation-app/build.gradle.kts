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
    id("com.android.application")
    id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin")
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.google.maps.android.compose.navigation"
    compileSdk = libs.versions.androidCompileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.google.maps.android.compose.navigation"
        minSdk = libs.versions.androidMinSdk.get().toInt()
        targetSdk = libs.versions.androidTargetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        manifestPlaceholders["MAPS_API_KEY"] = "DEFAULT_API_KEY"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        // Required by com.google.android.libraries.navigation:navigation AAR metadata when minSdk < 26
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    packaging {
        resources {
            pickFirsts += listOf(
                "META-INF/LICENSE.md",
                "META-INF/LICENSE-notice.md",
            )
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
            "-opt-in=kotlin.RequiresOptIn",
        )
    }
}

configurations.all {
    // IMPORTANT: Exclude play-services-maps because the Navigation SDK already bundles the Maps SDK.
    exclude(group = "com.google.android.gms", module = "play-services-maps")
}

dependencies {
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.core)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.preview.tooling)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended.android)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.test.compose.ui.manifest)

    // Instead of the lines below, regular apps would load these libraries from Maven according to
    // the README installation instructions
    implementation(project(":maps-compose"))
    implementation(project(":maps-compose-widgets"))
    implementation(project(":maps-compose-utils"))

    implementation(libs.maps.utils)
    implementation(libs.maps.utils.core)

    // Use the Navigation SDK which includes the Maps SDK
    implementation(libs.navigation)
    implementation(libs.places)
    implementation(libs.play.services.location)

    testImplementation(libs.test.junit)
    testImplementation(libs.androidx.core)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.test.junit.ktx)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.test.compose.ui)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.maps.testing.robolectric)
    testImplementation(libs.mockk)
    testImplementation(libs.robolectric)
    testImplementation(libs.truth)
}

secrets {
    // To add your Maps API key to this project:
    // 1. If the secrets.properties file does not exist, create it in the same folder as the local.properties file.
    // 2. Add this line, where YOUR_API_KEY is your API key:
    //        MAPS_API_KEY=YOUR_API_KEY
    propertiesFileName = "secrets.properties"

    // A properties file containing default secret values. This file can be
    // checked in version control.
    defaultPropertiesFileName = "local.defaults.properties"
}
