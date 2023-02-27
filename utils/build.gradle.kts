plugins {
    id("kmp-library-conventions")
}

kotlin.sourceSets.commonMain.get().dependencies {
    api(Dependencies.coroutinesCore)
    implementation(Dependencies.okio)
}