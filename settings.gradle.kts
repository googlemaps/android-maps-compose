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

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        if (providers.gradleProperty("useMavenLocal").orNull == "true" ||
            providers.environmentVariable("USE_MAVEN_LOCAL").orNull == "true") {
            mavenLocal()
        }
        google()
        mavenCentral()
    }
}
pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
rootProject.name = "android-maps-compose"

// The maps-compose-multiplatform module depends on the experimental KMP branch of
// android-maps-utils (feat/experimental-kmp-clustering), which provides multiplatform
// clustering algorithms and the common LatLng/CameraPosition model types.
//
// Default consumption path: publish those modules to mavenLocal and enable the
// mavenLocal repository (see dependencyResolutionManagement above):
//   (in ../android-maps-utils) ./gradlew :maps-model:publishToMavenLocal :clustering:publishToMavenLocal
//   (here)                     ./gradlew -PuseMavenLocal=true <task>
//
// Alternative for tight iteration: -PuseLocalMapsUtils=true substitutes the two
// coordinates with a composite build of ../android-maps-utils. Note that with the
// composite, platform compilations (android/iOS) work but the shared-metadata
// compilation (compileCommonMainKotlinMetadata, and thus assemble) fails: Kotlin's
// granular metadata transformation currently skips project dependencies substituted
// across included builds, so common code cannot be analyzed against them.
if (providers.gradleProperty("useLocalMapsUtils").orNull == "true") {
    includeBuild("../android-maps-utils") {
        dependencySubstitution {
            substitute(module("com.google.maps.android:clustering")).using(project(":clustering"))
            substitute(module("com.google.maps.android:maps-model")).using(project(":maps-model"))
        }
    }
}

include(":maps-app")
include(":maps-compose")
include(":maps-compose-widgets")
include(":maps-compose-utils")
include(":maps-compose-multiplatform")
include(":docs")