buildscript {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.7.20")
    }
}


allprojects {
    repositories {
        google()
        mavenCentral()
    }
}




// tasks.register("clean", Delete::class) {
//     delete(rootProject.buildDir)
// }

