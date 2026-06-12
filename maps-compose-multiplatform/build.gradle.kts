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

plugins {
    kotlin("multiplatform")
    kotlin("native.cocoapods")
    id("com.android.kotlin.multiplatform.library")
    alias(libs.plugins.compose.compiler)
}

kotlin {
    androidLibrary {
        namespace = "com.google.maps.android.compose.multiplatform"
        compileSdk = libs.versions.androidCompileSdk.get().toInt()
        minSdk = libs.versions.androidMinSdk.get().toInt()
    }
    
    // Enable iOS targets
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    cocoapods {
        summary = "Multiplatform Google Maps wrapper"
        homepage = "https://github.com/googlemaps/android-maps-compose"
        version = "1.0"
        ios.deploymentTarget = "16.0"
        pod("GoogleMaps") {
            version = "10.14.0.0"
        }
        framework {
            baseName = "maps_compose_multiplatform"
            isStatic = true
            linkerOpts("-lc++")
            linkerOpts("-framework", "Accelerate")
            linkerOpts("-framework", "CoreData")
            linkerOpts("-framework", "CoreGraphics")
            linkerOpts("-framework", "CoreImage")
            linkerOpts("-framework", "CoreLocation")
            linkerOpts("-framework", "CoreText")
            linkerOpts("-framework", "GLKit")
            linkerOpts("-framework", "ImageIO")
            linkerOpts("-framework", "Metal")
            linkerOpts("-framework", "OpenGLES")
            linkerOpts("-framework", "QuartzCore")
            linkerOpts("-framework", "Security")
            linkerOpts("-framework", "SystemConfiguration")
            linkerOpts("-framework", "UIKit")
        }
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation("org.jetbrains.compose.runtime:runtime:1.7.3")
                implementation("org.jetbrains.compose.foundation:foundation:1.7.3")
                implementation("org.jetbrains.compose.ui:ui:1.7.3")
            }
        }
        androidMain {
            dependencies {
                // Link the existing maps-compose module locally
                api(project(":maps-compose"))
            }
        }
        iosMain {
            dependencies {
                // Uses native MapKit via platform libraries
            }
        }
    }
}
