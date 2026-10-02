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

import com.vanniktech.maven.publish.MavenPublishBaseExtension
import org.gradle.api.Project

/**
 * Publishes to Maven Central with signing and the POM shared by every library in this
 * repository, Android-only and multiplatform alike.
 */
fun MavenPublishBaseExtension.configureMapsComposePublishing(
    project: Project,
    pomDescription: String = "Jetpack Compose components for the Maps SDK for Android",
) {
    publishToMavenCentral()
    signAllPublications()

    pom {
        name.set(project.name)
        description.set(pomDescription)
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
