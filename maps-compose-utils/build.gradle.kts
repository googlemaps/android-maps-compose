import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("kotlin-android")
    alias(libs.plugins.compose.compiler)
    id("android.maps.compose.PublishingConventionPlugin")
}

android {
    lint {
        sarifOutput = layout.buildDirectory.file("reports/lint-results.sarif").get().asFile
    }

    namespace = "com.google.maps.android.compose.utils"
    compileSdk = 36

    defaultConfig {
        minSdk = 21
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    buildFeatures {
        buildConfig = false
        compose = true
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_1_8)
            freeCompilerArgs.addAll(
                "-Xexplicit-api=strict",
                "-Xopt-in=kotlin.RequiresOptIn"
            )
        }
    }
}

dependencies {
    api(project(":maps-compose"))

    implementation(libs.androidx.core)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.kotlin)
    implementation(libs.kotlinx.coroutines.android)
    api(libs.maps.ktx.utils)
}
