plugins {
    id("kotlin-android")
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.google.maps.android.compose.utils"
    compileSdk = 34

    defaultConfig {
        minSdk = 21
        lint.targetSdk = 34
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    buildFeatures {
        buildConfig = false
        compose = true
    }

    kotlinOptions {
        jvmTarget = "1.8"
        freeCompilerArgs += "-Xexplicit-api=strict"
        freeCompilerArgs += "-Xopt-in=kotlin.RequiresOptIn"
    }
}

dependencies {
    api(project(":maps-compose"))

    implementation(libs.androidx.core)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.kotlin)
    api(libs.maps.ktx.utils)
}
