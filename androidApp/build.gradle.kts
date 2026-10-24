plugins {
    id("pacemaker-android-application")
}

android {
    namespace = "io.sellmair.pacemaker"
    compileSdk = 37

    defaultConfig {
        applicationId = "io.sellmair.pacemaker"
        minSdk = 31
        targetSdk = 36
        versionCode = 17
        versionName = "2025.1"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
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
    implementation(project(":app"))
    implementation(project(":app-core"))
    implementation(deps.androidx.activity.compose)
    implementation(deps.androidx.core.ktx)
    implementation(deps.coroutines.android)
    implementation(deps.evas)
    implementation(deps.evas.compose)
    implementation(deps.compose.runtime)
    implementation(deps.compose.ui.tooling.preview)
    debugImplementation(deps.compose.ui.tooling)
    androidTestImplementation(deps.androidx.test.runner)
    androidTestImplementation(deps.androidx.test.core)
    androidTestImplementation(deps.androidx.test.ext.junit)
}
