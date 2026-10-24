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
        implementation(Dependencies.androidx_annotation)
    }
}
