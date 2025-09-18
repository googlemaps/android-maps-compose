package internal

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.compose.compiler.gradle.ComposeCompilerGradlePluginExtension

/**
 * Configure Compose-specific options
 */
internal fun Project.configureAndroidCompose(
    commonExtension: CommonExtension<*, *, *, *, *, *>,
) {
    apply(plugin = "org.jetbrains.kotlin.plugin.compose")

    commonExtension.apply {
        buildFeatures {
            compose = true
        }

        dependencies {
            val bom = libs.findLibrary("androidx-compose-bom").get()
            "implementation"(platform(bom))
            "androidTestImplementation"(platform(bom))
            "implementation"(libs.findLibrary("androidx-compose-ui-preview-tooling").get())
            "debugImplementation"(libs.findLibrary("androidx-compose-ui-tooling").get())
        }
    }

//    extensions.configure<ComposeCompilerGradlePluginExtension> {
//        fun Provider<String>.onlyIfTrue() = flatMap { provider { it.takeIf(String::toBoolean) } }
//        fun Provider<*>.relativeToRootProject(dir: String) = map {
//            isolated.rootProject.projectDirectory
//                .dir("build")
//                .dir(projectDir.toRelativeString(rootDir))
//        }.map { it.dir(dir) }
//
//        project.providers.gradleProperty("composeCompilerMetrics").onlyIfTrue()
//            .relativeToRootProject("compose-metrics")
//            .let(metricsDestination::set)
//
//        project.providers.gradleProperty("composeCompilerReports").onlyIfTrue()
//            .relativeToRootProject("compose-reports")
//            .let(reportsDestination::set)
//
//        stabilityConfigurationFiles
//            .add(isolated.rootProject.projectDirectory.file("compose_compiler_config.conf"))
//    }
}
