@file:Suppress("UnstableApiUsage")

pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        google()
    }
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

        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev") {
            mavenContent {
                includeGroupByRegex(".*compose.*")
            }
        }
    }
}

include(":app")
include(":models")
include(":utils")
include(":bluetooth-core")
include(":bluetooth")
include(":spoof-tool")