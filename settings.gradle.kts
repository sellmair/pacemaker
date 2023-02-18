@file:Suppress("UnstableApiUsage")

pluginManagement {
    repositories {
        mavenCentral()
        google()
    }

    plugins {
        kotlin("jvm") version "1.8.20-Beta"
        kotlin("multiplatform") version "1.8.20-Beta"
        kotlin("android") version "1.8.20-Beta"
        id("com.android.application") version "7.4.0"
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

        maven("https://androidx.dev/storage/compose-compiler/repository") {
            mavenContent {
                includeGroupByRegex("androidx.compose.*")
            }
        }
    }
}

include("app")