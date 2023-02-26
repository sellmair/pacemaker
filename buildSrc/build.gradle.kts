plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    google {
        mavenContent {
            includeGroupByRegex(".*google.*")
            includeGroupByRegex(".*android.*")
        }
    }
}

dependencies {
    implementation(kotlin("gradle-plugin:1.8.20-Beta"))
    implementation("com.android.tools.build:gradle:7.4.1")
}