plugins {
    id("org.jetbrains.kotlin.android")
    alias(libs.plugins.compose.compiler)
    id("android.maps.compose.PublishingConventionPlugin")
}

android {

    namespace = "com.google.maps.android.compose"
    compileSdk = 34

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

    kotlinOptions {
        jvmTarget = "1.8"
        freeCompilerArgs += listOf(
            "-Xexplicit-api=strict",
            "-Xopt-in=kotlin.RequiresOptIn",
        )
    }
}

composeCompiler {
    stabilityConfigurationFile =
        layout.projectDirectory.file("compose_compiler_stability_config.conf")
    if (findProperty("composeCompilerReports") == "true") {
        reportsDestination = layout.buildDirectory.dir("compose_compiler")
    }
    if (findProperty("composeCompilerMetrics") == "true") {
        metricsDestination = layout.buildDirectory.dir("compose_compiler")
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.core)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.kotlin)
    implementation(libs.kotlinx.coroutines.android)
    api(libs.maps.ktx.std)

    testImplementation(libs.test.junit)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.test.espresso)
    androidTestImplementation(libs.androidx.test.junit.ktx)
}
