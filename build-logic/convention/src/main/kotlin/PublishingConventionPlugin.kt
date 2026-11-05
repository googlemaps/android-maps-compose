/*
 * Copyright 2026 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// buildSrc/src/main/kotlin/PublishingConventionPlugin.kt
import com.vanniktech.maven.publish.AndroidSingleVariantLibrary
import com.vanniktech.maven.publish.MavenPublishBaseExtension
import kotlinx.kover.gradle.plugin.dsl.KoverProjectExtension
import kotlinx.validation.KotlinApiBuildTask
import kotlinx.validation.KotlinApiCompareTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Copy
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.register

class PublishingConventionPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.run {
            applyPlugins()
            configureKover()
            configureVanniktechPublishing()
            configureBinaryCompatibilityValidator()
        }
    }

    private fun Project.applyPlugins() {
        apply(plugin = "com.android.library")
        apply(plugin = "org.jetbrains.dokka")
        apply(plugin = "org.jetbrains.kotlinx.kover")
        apply(plugin = "com.vanniktech.maven.publish")
    }

    private fun Project.configureKover() {
        configure<KoverProjectExtension> {
            reports {
                filters {
                    excludes {
                        androidGeneratedClasses()
                    }
                }
            }
        }
    }

    private fun Project.configureVanniktechPublishing() {
        extensions.configure<MavenPublishBaseExtension> {
            configure(
                AndroidSingleVariantLibrary(
                    variant = "release",
                    sourcesJar = true,
                    publishJavadocJar = true,
                ),
            )

            configureMapsComposePublishing(project)
        }
    }

    /**
     * Dumps the public API of the release classes to api/<module>.api and checks it on every build,
     * so a change to the public API shows up in review and an accidental break fails CI. Run
     * `./gradlew apiDump` after an intentional API change and commit the updated file.
     */
    private fun Project.configureBinaryCompatibilityValidator() {
        val projectName = name
        val apiFile = layout.projectDirectory.file("api/$projectName.api")
        val buildApiFile = layout.buildDirectory.file("api/$projectName.api")

        afterEvaluate {
            val bundleTask = tasks.findByName("bundleLibCompileToJarRelease") ?: return@afterEvaluate
            val classesJar = layout.buildDirectory.file(
                "intermediates/compile_library_classes_jar/release/bundleLibCompileToJarRelease/classes.jar",
            )

            val apiBuild = tasks.register<KotlinApiBuildTask>("apiBuild") {
                group = "verification"
                description = "Builds public API declaration for $projectName."
                inputJar.set(classesJar)
                outputApiFile.set(buildApiFile)
                // The generated R classes are public in bytecode but not part of the API.
                val rClasses = listOf(
                    "com.google.maps.android.compose.R",
                    "com.google.maps.android.compose.utils.R",
                    "com.google.maps.android.compose.widgets.R",
                )
                ignoredClasses.addAll(rClasses)
                ignoredClasses.addAll(rClasses.flatMap { r -> R_TYPES.map { type -> "$r$$type" } })
                dependsOn(bundleTask)
            }

            val apiDump = tasks.register<Copy>("apiDump") {
                group = "verification"
                description = "Syncs public API declarations of $projectName to the project api/ directory."
                from(apiBuild.flatMap { it.outputApiFile })
                into(apiFile.asFile.parentFile)
                dependsOn(apiBuild)
            }

            val apiCheck = tasks.register<KotlinApiCompareTask>("apiCheck") {
                group = "verification"
                description =
                    "Checks public API declarations of $projectName against the committed api/$projectName.api."
                projectApiFile.set(apiFile)
                generatedApiFile.set(apiBuild.flatMap { it.outputApiFile })
                dependsOn(apiBuild)
            }

            tasks.findByName("check")?.dependsOn(apiCheck)

            rootProject.tasks.matching { it.name == "apiDump" }.configureEach {
                dependsOn(apiDump)
            }
            rootProject.tasks.matching { it.name == "apiCheck" }.configureEach {
                dependsOn(apiCheck)
            }
        }
    }
}

private val R_TYPES = listOf(
    "anim", "animator", "array", "attr", "bool", "color", "dimen", "drawable", "font", "fraction", "id",
    "integer", "interpolator", "layout", "menu", "mipmap", "plurals", "raw", "string", "style", "styleable",
    "transition", "xml",
)
