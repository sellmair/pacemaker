plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()

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
}


dependencies {
    val kotlinVersion = "1.8.21"
    implementation("org.jetbrains.compose:compose-gradle-plugin:1.4.0")
    implementation(kotlin("gradle-plugin:$kotlinVersion"))
    implementation("com.android.tools.build:gradle:7.4.1")
}