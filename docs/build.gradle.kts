plugins {
    kotlin("jvm") apply false
    id("org.jetbrains.dokka")
}

dependencies {
    dokka(project(":maps-compose"))
    dokka(project(":maps-compose-utils"))
    dokka(project(":maps-compose-widgets"))
}

dokka {
    moduleName.set("Android Maps Compose")
}