@file:SuppressLint("TestManifestGradleConfiguration")

import android.annotation.SuppressLint
import com.android.build.api.dsl.ApplicationExtension
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    id("pacemaker-application")
    id("org.jetbrains.compose")
    kotlin("native.cocoapods")
}

pacemaker {
    ios()
    android()
}

extensions.configure(ApplicationExtension::class) {
    namespace = "io.sellmair.pacemaker"

    defaultConfig {
        versionName = "2023.2"
        versionCode = 10
    }
}

kotlin {
    sourceSets.commonMain.get().dependencies {
       implementation(project(":app-core"))

        /* COMPOSE */
        implementation(compose.ui)
        implementation(compose.foundation)
        implementation(compose.runtime)

        implementation(compose.material3)
        implementation(compose.materialIconsExtended)

        /* Utils */
        implementation("org.jetbrains.kotlinx:atomicfu:0.22.0")
    }

    sourceSets.androidMain.get().dependencies {
        /* androidx */
        implementation("androidx.activity:activity-compose:1.8.0")
        implementation("androidx.compose.ui:ui-tooling-preview:1.5.3")
        implementation(compose.preview)
    }

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    sourceSets.invokeWhenCreated("androidDebug") {
        dependencies {
            implementation("androidx.compose.ui:ui-tooling:1.5.3")
            implementation("androidx.compose.ui:ui-test-manifest:1.5.3")
        }
    }

    sourceSets.getByName("androidInstrumentedTest").dependencies {
        implementation("androidx.compose.ui:ui-test-junit4:1.5.3")
    }

    cocoapods {
        version = "2023.1"
        name = "PM"
        podfile = project.file("../iosApp/Podfile")

        framework {
            homepage = "https://github.com/sellmair/pacemaker"
            summary = "Application Framework"
            baseName = "PM"
            linkerOpts("-lsqlite3")
        }
    }
}
