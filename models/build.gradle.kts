plugins {
    id("kmp-library-conventions")
    kotlin("plugin.serialization")
}

kotlin {
    sourceSets.commonMain.get().dependencies {
        api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0-RC")
        api(Dependencies.okio)
    }
}