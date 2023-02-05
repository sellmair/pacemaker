@file:Suppress("UnstableApiUsage")

pluginManagement {
    repositories {
        mavenCentral()
        google()
    }

    plugins {
        kotlin("jvm") version "1.8.0"
        kotlin("multiplatform") version "1.8.0"
        kotlin("android") version "1.8.0"
        id("com.android.application") version "7.4.0-beta02"
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        google()

        /* Only used for polar SDK */
        maven("https://jitpack.io") {
            mavenContent {
                includeGroup("com.github.polarofficial")
            }
        }
    }
}

include("app")