@file:Suppress("UnstableApiUsage")

pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        google()
    }

    plugins {
        id("org.jetbrains.kotlin.plugin.serialization") version "1.8.20-Beta"
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

include(":app")
include(":models")
include(":utils")
include(":bluetooth")
include(":spoof-tool")