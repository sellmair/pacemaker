@file:Suppress("UnstableApiUsage")

pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        google()
        maven("https://maven.pkg.jetbrains.space/public/p/amper/amper")
    }
}

plugins {
    id("org.jetbrains.amper.settings.plugin").version("0.1.2")
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()

        google {
            mavenContent {
                includeGroupByRegex(".*android.*")
                includeGroupByRegex(".*androidx.*")
            }
        }

        maven("https://androidx.dev/storage/compose-compiler/repository") {
            mavenContent {
                includeGroupByRegex("androidx.compose.*")
            }
        }

        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev") {
            mavenContent {
                includeGroupByRegex(".*compose.*")
            }
        }
    }
}

/*
include(":utils")
include(":models")
include(":bluetooth")
include(":bluetooth-core")
include(":spoof-tool")
include(":app")
include(":app-core")
 */