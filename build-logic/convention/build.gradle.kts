plugins {
    `kotlin-dsl`
}

repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
}


dependencies {
    implementation(libs.kotlin.gradle.plugin)
    implementation(libs.android.gradle.plugin)
    compileOnly(libs.compose.gradle.plugin)
    implementation(libs.dokka.plugin)
    implementation(libs.org.jacoco.core)
    implementation(libs.gradle.maven.publish.plugin)
}

gradlePlugin {
    plugins {
        register("publishingConventionPlugin") {
            id = "android.maps.compose.publish"
            implementationClass = "PublishingConventionPlugin"
        }
        register("androidLibraryConventionPlugin") {
            id = "android.maps.compose.library"
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("androidApplicationConventionPlugin") {
            id = "android.maps.compose.application"
            implementationClass = "AndroidApplicationConventionPlugin"
        }
    }
}