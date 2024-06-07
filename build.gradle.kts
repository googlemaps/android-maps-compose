// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    val kotlinVersion by extra(libs.versions.kotlin.get())
    val composeCompilerVersion by extra(libs.versions.composecompiler.get())
    val androidxTestVersion by extra(libs.versions.androidxtest.get())
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath(libs.android.gradle.plugin)
        classpath(libs.maps.secrets.plugin)
        classpath(libs.kotlin.gradle.plugin)
        classpath(libs.dokka.plugin)
        classpath(libs.jacoco.android.plugin)
    }
}

plugins {
    alias(libs.plugins.dokka) apply true
    alias(libs.plugins.compose.compiler) apply false
}

val projectArtifactId by extra { project: Project ->
    if (project.name in listOf("maps-compose", "maps-compose-widgets", "maps-compose-utils")) {
        project.name
    } else {
        null
    }
}

allprojects {
    group = "com.google.maps.android"
    version = "5.0.1"
    val projectArtifactId by extra { project.name }
}

/**
 * Publishing and signing info
 */
subprojects {
    if (project.ext.has("artifactId") && project.ext["artifactId"] == null) return@subprojects

    apply(plugin = "com.android.application")
    apply(plugin = "com.mxalbert.gradle.jacoco-android")
    apply(plugin = "maven-publish")
    apply(plugin = "org.jetbrains.dokka")
    apply(plugin = "signing")

    // Code coverage
    configure<JacocoPluginExtension> {
        toolVersion = rootProject.libs.versions.jacoco.tool.plugin.get()
    }

    tasks.withType<Test>().configureEach {
        extensions.configure(JacocoTaskExtension::class.java) {
            isIncludeNoLocationClasses = true
            excludes = listOf("jdk.internal.*")
        }
    }

    configure<com.android.build.gradle.LibraryExtension> {
        publishing {
            singleVariant("release") {
                withSourcesJar()
                withJavadocJar()
            }
        }
    }
    project.extensions.configure<PublishingExtension> {
        publications {
            create<MavenPublication>("aar") {
                from(components["release"])
                pom {
                    name.set(project.name)
                    description.set("Jetpack Compose components for the Maps SDK for Android")
                    url.set("https://github.com/googlemaps/android-maps-compose")
                    scm {
                        connection.set("scm:git@github.com:googlemaps/android-maps-compose.git")
                        developerConnection.set("scm:git@github.com:googlemaps/android-maps-compose.git")
                        url.set("https://github.com/googlemaps/android-maps-compose")
                    }
                    licenses {
                        license {
                            name.set("The Apache Software License, Version 2.0")
                            url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                            distribution.set("repo")
                        }
                    }
                    organization {
                        name.set("Google Inc")
                        url.set("http://developers.google.com/maps")
                    }
                    developers {
                        developer {
                            name.set("Google Inc.")
                        }
                    }
                }
            }
        }
        repositories {
            maven {
                val releasesRepoUrl = uri("https://oss.sonatype.org/service/local/staging/deploy/maven2/")
                val snapshotsRepoUrl = uri("https://oss.sonatype.org/content/repositories/snapshots/")
                url = if (project.version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl
                credentials {
                    username = project.findProperty("sonatypeUsername") as String?
                    password = project.findProperty("sonatypePassword") as String?
                }
            }
        }
    }

    project.extensions.configure<SigningExtension> {
        sign(project.extensions.getByType<PublishingExtension>().publications["aar"])
    }
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}
