pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "ktxml"
include(":core")
includeBuild("convention-plugins")