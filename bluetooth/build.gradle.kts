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
        api(project(":bluetooth-core"))
        api(project(":models"))
        implementation(Dependencies.okio)
    }

    sourceSets.androidMain.dependencies {
        implementation(Dependencies.androidx_annotation)
    }
}
