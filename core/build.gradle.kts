plugins {
    kotlin("multiplatform")
    kotlin("native.cocoapods")
    id("com.android.library")
//    id("maven-publish")
    id("convention.publication")
}


group = "org.kobjects.kxml3"
version = "0.1.3"


kotlin {
    android()
    iosX64()
    iosArm64()
    //iosSimulatorArm64() sure all ios dependencies support this target
    jvm("desktop")
    js(IR) {
        //  useCommonJs()
        browser()
    }

    cocoapods {
        summary = "Kotlin version of Kxml"
        homepage = "https://github.com/kobjects/kxml3"
        ios.deploymentTarget = "14.1"
        framework {
            baseName = "kxml3"
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
        val androidMain by getting
        val androidTest by getting {
            dependencies {
                implementation(kotlin("test-junit"))
                implementation("junit:junit:4.13.2")
            }
        }
        val iosX64Main by getting
        val iosArm64Main by getting
        //val iosSimulatorArm64Main by getting
        val iosMain by creating {
            dependsOn(commonMain)
            iosX64Main.dependsOn(this)
            iosArm64Main.dependsOn(this)
            //iosSimulatorArm64Main.dependsOn(this)
        }
        val iosX64Test by getting
        val iosArm64Test by getting
        //val iosSimulatorArm64Test by getting
        val iosTest by creating {
            dependsOn(commonTest)
            iosX64Test.dependsOn(this)
            iosArm64Test.dependsOn(this)
            //iosSimulatorArm64Test.dependsOn(this)
        }

        val desktopMain by getting
        val desktopTest by getting

        val jsMain by getting
        val jsTest by getting

    }
}

android {
    compileSdk = 32
    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    defaultConfig {
        minSdk = 21
        targetSdk = 32
    }
}