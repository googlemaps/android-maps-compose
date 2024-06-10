plugins {
    `kotlin-dsl`
}

repositories {
    google()
    mavenCentral()
    gradlePluginPortal() // Optional, for other Gradle plugins
}


dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.8.0")
    implementation("com.android.tools.build:gradle:7.4.0")
    implementation("org.jetbrains.dokka:dokka-gradle-plugin:1.5.31")
    implementation("org.jacoco:org.jacoco.core:0.8.7")
}

gradlePlugin {
    plugins {
        register("publishingConventionPlugin") {
            id = "android.maps.compose.PublishingConventionPlugin"
            implementationClass = "PublishingConventionPlugin"
        }
    }
}
