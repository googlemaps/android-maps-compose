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
    implementation(libs.dokka.plugin)
    implementation(libs.org.jacoco.core)
}

gradlePlugin {
    plugins {
        register("publishingConventionPlugin") {
            id = "android.maps.compose.PublishingConventionPlugin"
            implementationClass = "PublishingConventionPlugin"
        }
    }
}