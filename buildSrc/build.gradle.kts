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
    val kotlinVersion = "2.1.0"
    implementation("org.jetbrains.compose:compose-gradle-plugin:1.7.3")
    implementation("org.jetbrains.kotlin.plugin.compose:org.jetbrains.kotlin.plugin.compose.gradle.plugin:$kotlinVersion")
    implementation(kotlin("gradle-plugin:$kotlinVersion"))
    implementation(kotlin("serialization:$kotlinVersion"))
    implementation("com.android.tools.build:gradle:8.7.3")
    implementation("app.cash.sqldelight:gradle-plugin:2.0.2")
    implementation("org.jetbrains.kotlinx:atomicfu-gradle-plugin:0.26.1")
}
