@file:Suppress("OPT_IN_USAGE_FUTURE_ERROR", "UnstableApiUsage")

import android.annotation.SuppressLint

plugins {
    kotlin("multiplatform")
    id("com.android.application")
}

android {
    compileSdk = 33
    defaultConfig {
        minSdk = 30
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.4.0"
    }
}

kotlin {
    android()

    sourceSets.getByName("commonMain").dependencies {
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    }

    sourceSets.getByName("androidMain").dependencies {
        implementation(platform("androidx.compose:compose-bom:2022.12.00"))
        implementation("androidx.activity:activity-compose:1.7.0-alpha04")
        implementation("androidx.compose.material3:material3")
        implementation("androidx.compose.ui:ui-tooling-preview")
    }

    sourceSets.invokeWhenCreated("androidDebug") {
        dependencies {
            implementation("androidx.compose.ui:ui-tooling")
            implementation("androidx.compose.ui:ui-test-manifest")
        }
    }

    sourceSets.getByName("androidInstrumentedTest").dependencies {
        implementation(platform("androidx.compose:compose-bom:2022.12.00"))
        implementation("androidx.compose.ui:ui-test-junit4")
    }
}
