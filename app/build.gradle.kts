@file:Suppress("UnstableApiUsage", "OPT_IN_IS_NOT_ENABLED")
@file:OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class, ExperimentalKotlinGradlePluginApi::class)
@file:SuppressLint("TestManifestGradleConfiguration")

import android.annotation.SuppressLint
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    id("kmp-application-conventions")
    id("org.jetbrains.compose")
    kotlin("native.cocoapods")
}

android {
    namespace = "io.sellmair.pacemaker"
}

kotlin {
    sourceSets.commonMain.get().dependencies {
        api(project(":models"))
        api(project(":utils"))
        api(project(":bluetooth"))

        /* COMPOSE */
        implementation(compose.ui)
        implementation(compose.foundation)
        implementation(compose.material)
        implementation(compose.runtime)

        implementation(compose.material3)
        implementation(compose.materialIconsExtended)

        /* Utils */
        implementation(Dependencies.coroutinesCore)
        implementation(Dependencies.okio)
        implementation("org.jetbrains.kotlinx:atomicfu:0.20.2")
    }

    sourceSets.androidMain.get().dependencies {
        /* androidx */
        implementation("androidx.activity:activity-compose:1.7.1")
        implementation("androidx.compose.ui:ui-tooling-preview:1.4.3")

        /* Polar SDK and dependencies */
        implementation("com.github.polarofficial:polar-ble-sdk:4.0.0")
        implementation("io.reactivex.rxjava3:rxjava:3.1.5")
        implementation("io.reactivex.rxjava3:rxandroid:3.0.0")

        /* kotlinx */
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactive:1.6.4")

    }

    sourceSets.invokeWhenCreated("androidDebug") {
        dependencies {
            implementation("androidx.compose.ui:ui-tooling:1.4.3")
            implementation("androidx.compose.ui:ui-test-manifest:1.4.3")
        }
    }

    sourceSets.androidInstrumentedTest.get().dependencies {
        implementation("androidx.compose.ui:ui-test-junit4:1.4.3")
    }

    cocoapods {
        version = "2023.1"
        name = "PM"
        podfile = project.file("../iosApp/Podfile")

        framework {
            homepage = "https://github.com/sellmair/pacemaker"
            summary = "Application Framework"
            baseName = "PM"
            isStatic = true
        }
    }
}

compose {
    kotlinCompilerPlugin.set("androidx.compose.compiler:compiler:1.4.7")
}
