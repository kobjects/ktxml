buildscript {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.1.21")
    }
}


allprojects {
    repositories {
        google()
        mavenCentral()
    }
}
