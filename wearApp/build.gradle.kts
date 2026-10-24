plugins {
    id("pacemaker-android-application")
}

android {
    namespace = "io.sellmair.pacemaker.wear"
    compileSdk = 37
    defaultConfig {
        applicationId = "io.sellmair.pacemaker"
        minSdk = 31
        targetSdk = 36
        versionCode = 1
        versionName = "2026.1"
    }
    buildFeatures { compose = true }
    buildTypes {
        create("profile") {
            initWith(getByName("release"))
            signingConfig = signingConfigs.getByName("debug")
            isDebuggable = false
            isProfileable = true
            matchingFallbacks += listOf("release")
        }
    }
}

dependencies {
    implementation(project(":app-core"))
    implementation(deps.androidx.activity.compose)
    implementation(deps.androidx.core.ktx)
    implementation(deps.health.services.client)
    implementation(deps.play.services.wearable)
    // Wearable Play Services still brings Fragment 1.1 transitively; Activity Result APIs
    // require a newer Fragment runtime even though Wear does not use fragment UI directly.
    implementation("androidx.fragment:fragment:1.8.9")
    implementation(deps.evas)
    implementation(deps.coroutines.android)
    implementation(deps.evas.compose)
    implementation(deps.compose.runtime)
    implementation(deps.compose.ui)
    implementation(deps.compose.foundation)
    implementation(deps.compose.material3)
    implementation("androidx.wear.compose:compose-material3:1.5.0")
}
