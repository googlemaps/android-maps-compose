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

import com.android.build.api.dsl.Lint
import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinMultiplatform
import com.vanniktech.maven.publish.MavenPublishBaseExtension
import com.vanniktech.maven.publish.SourcesJar
import kotlinx.kover.gradle.plugin.dsl.KoverProjectExtension
import kotlinx.validation.KotlinApiBuildTask
import kotlinx.validation.KotlinApiCompareTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.register
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation

/**
 * Conventions for the Kotlin Multiplatform libraries in this repository: Android and iOS targets,
 * explicit API mode, ABI validation of every target, lint, coverage and Maven Central publishing.
 *
 * iOS klibs can only be built on macOS, so publishing a module that applies this plugin needs a
 * macOS runner.
 */
class KmpPublishingConventionPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.run {
            apply(plugin = "org.jetbrains.kotlin.multiplatform")
            apply(plugin = "com.android.kotlin.multiplatform.library")
            // Multiplatform Android libraries only get lint tasks from the standalone lint plugin.
            apply(plugin = "com.android.lint")
            apply(plugin = "org.jetbrains.kotlinx.kover")
            apply(plugin = "org.jetbrains.dokka")
            apply(plugin = "com.vanniktech.maven.publish")

            configure<KotlinMultiplatformExtension> {
                explicitApi()
                // No iosX64 (Intel simulators): Compose Multiplatform dropped it in 1.11.
                iosArm64()
                iosSimulatorArm64()

                // Records the public API of the iOS klibs in api/. The Android target is not
                // supported yet, see configureAndroidApiValidation. Run updateKotlinAbi after an
                // intended API change; checkKotlinAbi runs with check.
                @OptIn(ExperimentalAbiValidation::class)
                abiValidation()
            }

            configure<Lint> {
                sarifOutput = layout.buildDirectory.file("reports/lint-results.sarif").get().asFile
            }

            configure<KoverProjectExtension> {
                // CI and the coverage history read koverXmlReportDebug / reportDebug.xml, which
                // the Android-only modules produce. Expose the KMP Android coverage the same way.
                currentProject {
                    createVariant("debug") {
                        add("android")
                    }
                }
                reports {
                    filters {
                        excludes {
                            androidGeneratedClasses()
                        }
                    }
                }
            }

            configure<MavenPublishBaseExtension> {
                configure(
                    KotlinMultiplatform(
                        javadocJar = JavadocJar.Dokka("dokkaGeneratePublicationHtml"),
                        sourcesJar = SourcesJar.Sources(),
                    )
                )
                configureMapsComposePublishing(
                    project,
                    pomDescription = "Compose Multiplatform components for the Maps SDKs for Android and iOS",
                )
            }

            configureAndroidApiValidation()
        }
    }

    /**
     * Kotlin's ABI validation does not cover the Android target of a multiplatform library, so
     * dump its public API from the compiled classes jar with the binary-compatibility-validator
     * tasks, into api/<name>.api, and hook them into updateKotlinAbi and checkKotlinAbi.
     */
    private fun Project.configureAndroidApiValidation() {
        val projectName = name
        val apiFile = layout.projectDirectory.file("api/$projectName.api")
        val buildApiFile = layout.buildDirectory.file("api/android/$projectName.api")

        afterEvaluate {
            val bundleTask = tasks.findByName("bundleAndroidMainClassesToCompileJar") ?: return@afterEvaluate
            val classesJar = layout.buildDirectory.file(
                "intermediates/compile_library_classes_jar/androidMain/bundleAndroidMainClassesToCompileJar/classes.jar"
            )

            val androidApiBuild = tasks.register<KotlinApiBuildTask>("androidApiBuild") {
                group = "verification"
                description = "Builds the public Android API declaration for $projectName."
                inputJar.set(classesJar)
                outputApiFile.set(buildApiFile)
                dependsOn(bundleTask)
            }

            // Writes only its own file: a Copy into api/ would claim the directory that the klib
            // dump also writes to.
            val androidApiDump = tasks.register("androidApiDump") {
                group = "verification"
                description = "Syncs the public Android API declaration of $projectName to api/$projectName.api."
                val generated = androidApiBuild.flatMap { it.outputApiFile }
                inputs.file(generated)
                outputs.file(apiFile)
                doLast {
                    generated.get().asFile.copyTo(apiFile.asFile, overwrite = true)
                }
            }

            val androidApiCheck = tasks.register<KotlinApiCompareTask>("androidApiCheck") {
                group = "verification"
                description = "Checks the public Android API of $projectName against api/$projectName.api."
                projectApiFile.set(apiFile)
                generatedApiFile.set(androidApiBuild.flatMap { it.outputApiFile })
                dependsOn(androidApiBuild)
                mustRunAfter(androidApiDump)
            }

            tasks.named("updateKotlinAbi") { dependsOn(androidApiDump) }
            tasks.named("checkKotlinAbi") { dependsOn(androidApiCheck) }
        }
    }
}
