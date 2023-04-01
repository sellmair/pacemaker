plugins {
    id("kmp-library-conventions")
    id("com.android.library")
}

android {
    namespace = "io.sellmair.pacemaker.bluetooth"
}

kotlin.sourceSets.commonMain.get().dependencies {
    api(project(":models"))
    api(project(":utils"))
    api(Dependencies.coroutinesCore)
    implementation(Dependencies.okio)
}

kotlin.sourceSets.androidMain.get().dependencies {
    implementation("androidx.annotation:annotation:1.6.0")
}