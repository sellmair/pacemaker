@file:SuppressLint("TestManifestGradleConfiguration")
@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

import android.annotation.SuppressLint
import com.android.build.api.dsl.ApplicationExtension
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    id("pacemaker-application")
    id("org.jetbrains.compose")
    kotlin("native.cocoapods")
    kotlin("plugin.compose")
}

pacemaker {
    ios()
    android()
}

extensions.configure(ApplicationExtension::class) {
    namespace = "io.sellmair.pacemaker"

    defaultConfig {
        versionName = "2024.2"
        versionCode = 15
    }
}

kotlin {
    compilerOptions {
        optIn.add("org.jetbrains.compose.resources.ExperimentalResourceApi")
    }

    sourceSets.commonMain.get().dependencies {
        implementation(project(":app-core"))

        implementation(Dependencies.evasCompose)

        /* COMPOSE */
        implementation(compose.ui)
        implementation(compose.foundation)
        implementation(compose.runtime)
        implementation(compose.components.resources)
        implementation(compose.components.uiToolingPreview)

        implementation(compose.material3)
        implementation(compose.materialIconsExtended)

    }

    sourceSets.androidMain.get().dependencies {
        /* androidx */
        implementation("androidx.activity:activity-compose:1.9.0")
        implementation(compose.preview)
    }

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    sourceSets.invokeWhenCreated("androidDebug") {
        dependencies {
            implementation(compose.uiTooling)
        }
    }

    sourceSets.getByName("androidInstrumentedTest").dependencies {
        implementation("androidx.compose.ui:ui-test-junit4:1.6.8")
    }

    cocoapods {
        version = "2024.2"
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
