// buildSrc/src/main/kotlin/PublishingConventionPlugin.kt
import com.vanniktech.maven.publish.AndroidSingleVariantLibrary
import com.vanniktech.maven.publish.MavenPublishBaseExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.*
import org.gradle.testing.jacoco.plugins.JacocoPluginExtension
import org.gradle.api.tasks.testing.Test
import org.gradle.testing.jacoco.plugins.JacocoTaskExtension
import org.gradle.testing.jacoco.tasks.JacocoReport

class PublishingConventionPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.run {

            applyPlugins()
            configureJacoco()
            configureVanniktechPublishing()
        }
    }

    private fun Project.applyPlugins() {
        apply(plugin = "com.android.library")
        apply(plugin = "org.jetbrains.dokka")
        apply(plugin = "org.gradle.jacoco")
        apply(plugin = "com.vanniktech.maven.publish")
    }

    private fun Project.configureJacoco() {
        configure<JacocoPluginExtension> {
            toolVersion = "0.8.11" // Compatible with newer JDKs
        }

        // AGP 9.0+ built-in Jacoco support or manual configuration.
        // We create a "jacocoTestReport" task to match the CI workflow.
        
        tasks.register<JacocoReport>("jacocoTestReport") {
             // Dependencies
             dependsOn("testDebugUnitTest")
             
             reports {
                 xml.required.set(true)
                 html.required.set(true)
             }
             
             // Source directories
             val mainSrc = "${layout.projectDirectory}/src/main/java"
             sourceDirectories.setFrom(files(mainSrc))
             
             // Class directories - we need to point to where Kotlin compiles to
             val debugTree = fileTree("${layout.buildDirectory.get()}/tmp/kotlin-classes/debug")
             classDirectories.setFrom(files(debugTree))
             
             // Execution data from the unit test task
             executionData.setFrom(fileTree(layout.buildDirectory.get()) {
                 include("outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec")
             })
        }
    }

    private fun Project.configureVanniktechPublishing() {
        extensions.configure<MavenPublishBaseExtension> {
            configure(
                AndroidSingleVariantLibrary(
                    variant = "release",
                    sourcesJar = true,
                    publishJavadocJar = true
                )
            )

            publishToMavenCentral()
            signAllPublications()

            pom {
                name.set(project.name)
                description.set("Jetpack Compose components for the Maps SDK for Android")
                url.set("https://github.com/googlemaps/android-maps-compose")
                licenses {
                    license {
                        name.set("The Apache Software License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                        distribution.set("repo")
                    }
                }
                scm {
                    connection.set("scm:git@github.com:googlemaps/android-maps-compose.git")
                    developerConnection.set("scm:git@github.com:googlemaps/android-maps-compose.git")
                    url.set("https://github.com/googlemaps/android-maps-compose")
                }
                developers {
                    developer {
                        id.set("google")
                        name.set("Google Inc.")
                    }
                }
                organization {
                    name.set("Google Inc")
                    url.set("http://developers.google.com/maps")
                }
            }
        }
    }
}