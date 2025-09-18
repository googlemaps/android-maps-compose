plugins {
    id("android.maps.compose.library")
    id("android.maps.compose.publish")
}

android {
    namespace = "com.google.maps.android.compose.utils"
}

dependencies {
    api(project(":maps-compose"))

//    implementation(libs.androidx.core)
    implementation(libs.androidx.compose.ui)
    implementation(libs.kotlin)
    implementation(libs.kotlinx.coroutines.android)
    api(libs.maps.ktx.utils)
}
