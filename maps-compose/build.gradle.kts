plugins {
    id("org.jetbrains.kotlin.android")
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.google.maps.android.compose"
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
        val stabilityConfigurationFile = layout.projectDirectory.file("compose_compiler_stability_config.conf").asFile
        freeCompilerArgs += listOf(
            "-Xexplicit-api=strict",
            "-Xopt-in=kotlin.RequiresOptIn",
            "-P",
            "plugin:androidx.compose.compiler.plugins.kotlin:stabilityConfigurationPath=${stabilityConfigurationFile.absolutePath}"
        )
        if (findProperty("composeCompilerReports") == "true") {
            freeCompilerArgs += listOf(
                "-P",
                "plugin:androidx.compose.compiler.plugins.kotlin:reportsDestination=${layout.buildDirectory.dir("compose_compiler").get()}",
            )
        }
        if (findProperty("composeCompilerMetrics") == "true") {
            freeCompilerArgs += listOf(
                "-P",
                "plugin:androidx.compose.compiler.plugins.kotlin:metricsDestination=${layout.buildDirectory.dir("compose_compiler").get()}",
            )
        }
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.core)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.kotlin)
    api(libs.maps.ktx.std)

    testImplementation(libs.test.junit)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.test.espresso)
    androidTestImplementation(libs.androidx.test.junit.ktx)
}
