plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin")
}

android {
    namespace = "com.google.maps.android.compose.navigation"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.google.maps.android.compose.navigation"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
        freeCompilerArgs += "-Xopt-in=kotlin.RequiresOptIn"
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }
}

configurations.all {
    resolutionStrategy {
        exclude(group = "com.google.android.gms", module = "play-services-maps")
    }
}

dependencies {

    implementation(libs.androidx.core)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.compose.activity)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.compose.ui.preview.tooling)
    implementation(libs.androidx.material3)
    testImplementation(libs.test.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.test.espresso)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.test.compose.ui)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // Instead of the lines below, regular apps would load these libraries from Maven according to
    // the README installation instructions
    implementation(project(":maps-compose"))
    implementation(project(":maps-compose-widgets"))
    implementation(project(":maps-compose-utils"))

    implementation(libs.maps.ktx.std)
    implementation(libs.maps.ktx.utils)

    // Use the navigation SDK which includes the maps SDK
    implementation(libs.navigation)

    implementation(libs.play.services.location)

    testImplementation(libs.androidx.core)
    testImplementation(libs.truth)

    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    implementation(libs.places)

    // Accompanist permission helper
    implementation(libs.accompanist.permissions)
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
