plugins {
    id("kmp-library-conventions")
}

kotlin.sourceSets.commonMain.get().dependencies {
    api(Dependencies.coroutinesCore)
}