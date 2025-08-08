@file:Suppress("UnstableApiUsage")

pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        google()
        maven("https://repo.sellmair.io")

    }
}

dependencyResolutionManagement {
    versionCatalogs.register("deps") {
        from(files("dependencies.toml"))
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

        maven("https://repo.sellmair.io")
    }
}

include(":utils")
include(":models")
include(":bluetooth")
include(":bluetooth-core")
include(":spoof-tool")
include(":app")
include(":app-core")
