/**
 * Common conventions for generating documentation with Dokka.
 */

plugins {
    id("org.jetbrains.dokka")
}

dokka {
    dokkaSourceSets.configureEach {
        sourceLink {
            localDirectory.set(rootDir)
        }
    }
}