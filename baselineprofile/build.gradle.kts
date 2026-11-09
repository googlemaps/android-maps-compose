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

// Generates the Baseline Profiles shipped with maps-compose, maps-compose-utils and
// maps-compose-widgets by running the maps-app demos on a device. See BaselineProfileGenerator.
plugins {
    alias(libs.plugins.android.test)
    id("androidx.baselineprofile")
}

android {
    namespace = "com.google.maps.android.compose.baselineprofile"
    compileSdk = libs.versions.androidCompileSdk.get().toInt()

    defaultConfig {
        // Macrobenchmark and the Baseline Profile rule need API 28.
        minSdk = 28
        targetSdk = libs.versions.androidTargetSdk.get().toInt()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    targetProjectPath = ":maps-app"
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

baselineProfile {
    // Run on the attached device or emulator. It needs a real MAPS_API_KEY so the maps load.
    useConnectedDevices = true
}

dependencies {
    implementation(libs.androidx.test.junit.ktx)
    implementation(libs.androidx.test.uiautomator)
    implementation(libs.androidx.benchmark.macro.junit4)
}
