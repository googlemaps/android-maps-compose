plugins {
    id("android.maps.compose.library")
    id("android.maps.compose.publish")
}

android {
    lint {
        sarifOutput = layout.buildDirectory.file("reports/lint-results.sarif").get().asFile
    }

    packaging {
        resources {
            excludes += "META-INF/LICENSE.md"
            excludes += "META-INF/LICENSE-notice.md"
        }
    }

    namespace = "com.google.maps.android.compose.widgets"
    compileSdk = 36

    defaultConfig {
        minSdk = 21
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
    implementation(project(":maps-compose"))

    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material)
//    implementation(libs.androidx.core)
    implementation(libs.kotlin)
    implementation(libs.kotlinx.coroutines.android)
    api(libs.maps.ktx.std)
    api(libs.maps.ktx.utils)

    testImplementation(libs.test.junit)
    androidTestImplementation(libs.androidx.test.espresso)
    androidTestImplementation(libs.androidx.test.junit.ktx)
    androidTestImplementation(libs.mockk)
    androidTestImplementation(libs.truth)
}
