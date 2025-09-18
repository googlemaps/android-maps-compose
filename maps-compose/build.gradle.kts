plugins {
    id("android.maps.compose.library")
    id("android.maps.compose.publish")
}

android {
    namespace = "com.google.maps.android.compose"

    sourceSets["main"].java.srcDir("build/generated/source/artifactId")
}

composeCompiler {
    stabilityConfigurationFiles.set(
        listOf(layout.projectDirectory.file("compose_compiler_stability_config.conf"))
    )

    if (findProperty("composeCompilerReports") == "true") {
        reportsDestination = layout.buildDirectory.dir("compose_compiler")
    }
    if (findProperty("composeCompilerMetrics") == "true") {
        metricsDestination = layout.buildDirectory.dir("compose_compiler")
    }
}


dependencies {
    implementation(libs.androidx.core)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.kotlin)
    implementation(libs.kotlinx.coroutines.android)
    api(libs.maps.ktx.std)

    testImplementation(libs.test.junit)

    androidTestImplementation(libs.androidx.test.espresso)
    androidTestImplementation(libs.androidx.test.junit.ktx)
}

// Artifact ID logic
val attributionId = "gmp_git_androidmapscompose_v$version"

val generateArtifactIdFile = tasks.register("generateArtifactIdFile") {
    val outputDir = layout.buildDirectory.dir("generated/source/artifactId")
    val packageName = "com.google.maps.android.compose.meta"
    val packagePath = packageName.replace('.', '/')
    val outputFile = outputDir.get().file("$packagePath/ArtifactId.kt").asFile

    outputs.file(outputFile)

    doLast {
        outputFile.parentFile.mkdirs()
        outputFile.writeText(
            """
            package $packageName

            public object AttributionId {
                public const val VALUE: String = "$attributionId"
            }
            """.trimIndent()
        )
    }
}

tasks.named("preBuild") {
    dependsOn(generateArtifactIdFile)
}
