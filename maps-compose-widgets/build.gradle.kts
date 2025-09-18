plugins {
    id("android.maps.compose.library")
    id("android.maps.compose.publish")
}

android {
    namespace = "com.google.maps.android.compose.widgets"
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
}
