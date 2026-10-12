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
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
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

                // Records the public API of every target: the iOS klibs in api/<name>.klib.api
                // and Android in api/android/<name>.api. Run updateKotlinAbi after an intended
                // API change; checkKotlinAbi runs with check.
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
        }
    }
}
