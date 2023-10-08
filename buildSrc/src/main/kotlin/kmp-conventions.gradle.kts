@file:Suppress("OPT_IN_USAGE")

plugins {
    kotlin("multiplatform")
    id("android-conventions")
}


kotlin {
    iosX64()
    iosArm64()
    iosSimulatorArm64()
    macosX64()
    macosArm64()

    targetHierarchy.default {
        common {
            group("mobile") {
                group("ios")
                withAndroidTarget()
                withJvm()
            }
        }
    }

    sourceSets.all {
        languageSettings.optIn("kotlin.time.ExperimentalTime")
        languageSettings.optIn("kotlinx.cinterop.ExperimentalForeignApi")
    }

    jvmToolchain(8)
}