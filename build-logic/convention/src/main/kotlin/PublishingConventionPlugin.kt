// buildSrc/src/main/kotlin/PublishingConventionPlugin.kt
import com.vanniktech.maven.publish.AndroidSingleVariantLibrary
import com.vanniktech.maven.publish.MavenPublishBaseExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.*
import org.gradle.testing.jacoco.plugins.JacocoPluginExtension
import org.gradle.api.tasks.testing.Test
import org.gradle.testing.jacoco.plugins.JacocoTaskExtension

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
        apply(plugin = "com.mxalbert.gradle.jacoco-android")
        apply(plugin = "org.jetbrains.dokka")
        apply(plugin = "com.vanniktech.maven.publish")
    }

    private fun Project.configureJacoco() {
        configure<JacocoPluginExtension> {
            toolVersion = "0.8.7"

        }

        tasks.withType<Test>().configureEach {
            extensions.configure(JacocoTaskExtension::class.java) {
                isIncludeNoLocationClasses = true
                excludes = listOf("jdk.internal.*")
            }
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