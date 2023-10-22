plugins {
    `kotlin-dsl`
}

repositories {
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev") {
        mavenContent {
            includeGroupByRegex(".*compose.*")
        }
    }

    google {
        mavenContent {
            includeGroupByRegex(".*google.*")
            includeGroupByRegex(".*android.*")
        }
    }

    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    val kotlinVersion = "1.9.20-RC"
    implementation("org.jetbrains.compose:compose-gradle-plugin:1.5.10-rc01")
    implementation(kotlin("gradle-plugin:$kotlinVersion"))
    implementation(kotlin("serialization:1.9.0"))
    implementation("com.android.tools.build:gradle:8.1.2")
    implementation("app.cash.sqldelight:gradle-plugin:2.0.0")
}
