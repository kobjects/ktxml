import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget


plugins {
    kotlin("multiplatform")
    kotlin("native.cocoapods")
    id("convention.publication")
    id("org.jetbrains.dokka") version "1.9.20"
}


group = "org.kobjects.ktxml"
version = "0.3.2"


tasks.dokkaHtml {
    moduleName.set("KtXml")
    outputDirectory.set(layout.buildDirectory.dir("dokka"))
    dokkaSourceSets {
        configureEach {
            includes.from("module.md")
        }
    }
}


kotlin {
    jvm {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_1_8
        }
    }

    js(IR) {
        browser()
        nodejs()
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        nodejs()
        d8()
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmWasi {
        nodejs()
    }

    // https://kotlinlang.org/docs/native-target-support.html#tier-1
    macosX64()
    macosArm64()
    iosSimulatorArm64()
    iosX64()
    iosArm64()

    // https://kotlinlang.org/docs/native-target-support.html#tier-2
    linuxX64()
    linuxArm64()
    watchosSimulatorArm64()
    watchosX64()
    watchosArm32()
    watchosArm64()
    tvosSimulatorArm64()
    tvosX64()
    tvosArm64()

    // https://kotlinlang.org/docs/native-target-support.html#tier-3
    mingwX64()
    watchosDeviceArm64()
    androidNativeX64()
    androidNativeArm32()
    androidNativeArm64()

    applyDefaultHierarchyTemplate()

    cocoapods {
        summary = "Kotlin version of Kxml"
        homepage = "https://github.com/kobjects/ktxml"
        ios.deploymentTarget = "14.1"
        framework {
            baseName = "ktxml"
        }
    }

    sourceSets {
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
