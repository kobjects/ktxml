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
    iosX64()
    iosArm64()
    iosSimulatorArm64()
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "1.8"
        }
    }
    js(IR) {
        //  useCommonJs()
        browser()
    }

    cocoapods {
        summary = "Kotlin version of Kxml"
        homepage = "https://github.com/kobjects/ktxml"
        ios.deploymentTarget = "14.1"
        framework {
            baseName = "ktxml"
        }
    }
    
    sourceSets {
        val commonMain by getting
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }

        val iosX64Main by getting
        val iosArm64Main by getting
        val iosSimulatorArm64Main by getting
        val iosMain by creating {
            dependsOn(commonMain)
            iosX64Main.dependsOn(this)
            iosArm64Main.dependsOn(this)
            iosSimulatorArm64Main.dependsOn(this)
        }
        val iosX64Test by getting
        val iosArm64Test by getting
        val iosSimulatorArm64Test by getting
        val iosTest by creating {
            dependsOn(commonTest)
            iosX64Test.dependsOn(this)
            iosArm64Test.dependsOn(this)
            iosSimulatorArm64Test.dependsOn(this)
        }

        val jvmMain by getting
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test-junit"))
            }
        }

        val jsMain by getting
        val jsTest by getting {
            dependencies {
                implementation(kotlin("test-js"))
            }
        }

    }
}
