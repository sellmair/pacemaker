plugins {
    id("pacemaker-library")
}

pacemaker {
    android()
    ios()
    macos()
    jvm()
}

kotlin {
    sourceSets.commonMain.dependencies {
        api(project(":models"))
    }

    sourceSets.androidMain.dependencies {
        implementation("androidx.annotation:annotation:1.7.0")
    }
}
