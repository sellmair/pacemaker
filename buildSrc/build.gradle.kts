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
    val kotlinVersion = "1.9.23"
    implementation("org.jetbrains.compose:compose-gradle-plugin:1.6.1")

    implementation(kotlin("gradle-plugin:$kotlinVersion"))
    implementation(kotlin("serialization:$kotlinVersion"))
    implementation("com.android.tools.build:gradle:8.1.4")
    implementation("app.cash.sqldelight:gradle-plugin:2.0.1")
    implementation("org.jetbrains.kotlinx:atomicfu-gradle-plugin:0.23.2")
}
