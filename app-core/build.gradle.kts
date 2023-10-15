plugins {
    id("pacemaker-library")
}


pacemaker {
    ios()
    android()
}

kotlin {
    sourceSets.commonMain.dependencies {
        api(project(":models"))
        api(project(":utils"))
        api(project(":bluetooth"))
    }

    sourceSets.androidMain.dependencies {
        implementation("androidx.core:core-ktx:1.12.0")
    }
}