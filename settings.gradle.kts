@file:Suppress("UnstableApiUsage")

pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        google()
    }

    plugins {
        kotlin("multiplatform") version "1.8.20-Beta"
        id("org.jetbrains.kotlin.plugin.serialization") version "1.8.20-Beta"
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