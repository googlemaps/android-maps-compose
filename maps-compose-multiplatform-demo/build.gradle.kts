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

// Demo screens for maps-compose-multiplatform, shared by the Android demo app (maps-app) and the
// iOS demo app (iosApp). Not published.
plugins {
    kotlin("multiplatform")
    kotlin("native.cocoapods")
    id("com.android.kotlin.multiplatform.library")
    alias(libs.plugins.compose.compiler)
}

kotlin {
    android {
        namespace = "com.google.maps.android.compose.multiplatform.demo"
        compileSdk = libs.versions.androidCompileSdk.get().toInt()
        minSdk = libs.versions.androidMinSdk.get().toInt()
    }

    iosArm64()
    iosSimulatorArm64()

    cocoapods {
        summary = "Demos of maps-compose-multiplatform"
        homepage = "https://github.com/googlemaps/android-maps-compose"
        version = project.version.toString()
        ios.deploymentTarget = "16.0"
        pod("GoogleMaps") {
            version = "10.14.0.0"
        }
        framework {
            baseName = "maps_compose_multiplatform_demo"
            isStatic = true
        }
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(project(":maps-compose-multiplatform"))
                implementation(libs.compose.multiplatform.runtime)
                implementation(libs.compose.multiplatform.foundation)
                implementation(libs.compose.multiplatform.ui)
            }
        }
    }
}
