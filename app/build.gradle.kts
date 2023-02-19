@file:Suppress("OPT_IN_USAGE_FUTURE_ERROR", "UnstableApiUsage")

plugins {
    kotlin("multiplatform")
    id("com.android.application")
    kotlin("plugin.serialization")
}

android {
    compileSdk = 33
    defaultConfig {
        minSdk = 31
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.4.3-dev-k1.8.20-Beta-15b4f4328eb"
    }
}

kotlin {
    android()
    iosArm64()

    sourceSets.getByName("commonMain").dependencies {
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
        implementation("com.squareup.okio:okio:3.3.0")
        implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0-RC")
    }

    sourceSets.getByName("androidMain").dependencies {
        /* androidx */
        implementation(platform("androidx.compose:compose-bom:2022.12.00"))
        implementation("androidx.activity:activity-compose:1.7.0-alpha04")
        implementation("androidx.compose.material3:material3")
        implementation("androidx.compose.ui:ui-tooling-preview")

        /* Polar SDK and dependencies */
        implementation("com.github.polarofficial:polar-ble-sdk:4.0.0")
        implementation("io.reactivex.rxjava3:rxjava:3.1.5")
        implementation("io.reactivex.rxjava3:rxandroid:3.0.0")

        /* kotlinx */
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactive:1.6.4")

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

    /* Requried for 1.8.20-Beta */
    android().compilations.all {
        kotlinOptions {
            kotlinOptions {
                freeCompilerArgs += listOf(
                    "-P", "plugin:androidx.compose.compiler.plugins.kotlin:suppressKotlinVersionCompatibilityCheck=true"
                )
            }
        }
    }

    sourceSets.all {
        languageSettings.optIn("kotlin.time.ExperimentalTime")
    }
}
