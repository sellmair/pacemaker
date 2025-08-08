@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

import org.jetbrains.compose.ComposePlugin
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    id("org.jetbrains.kotlin.multiplatform")
}


kotlin {
    jvmToolchain(21)

    sourceSets.commonMain.dependencies {
        if (project.name != "utils") {
            implementation(project(":utils"))
        }

        implementation(Dependencies.coroutines_core)
        implementation(Dependencies.okio)
        implementation(Dependencies.kotlinx_datetime)
        implementation(Dependencies.kotlinx_collections_immutable)
        implementation(ComposePlugin.Dependencies(project).runtime)
        implementation(Dependencies.evas)
    }

    sourceSets.commonTest.dependencies {
        implementation(kotlin("test"))
        implementation(kotlin("test-common"))
        implementation(kotlin("test-annotations-common"))
        implementation(Dependencies.coroutines_test)
    }


    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
        optIn.add("kotlin.time.ExperimentalTime")
    }
}

tasks.withType<Test>().configureEach {
    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = true
        showExceptions = true
        showStackTraces = true
        showCauses = true
        outputs.upToDateWhen { false }
    }
}
