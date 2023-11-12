plugins {
    id("pacemaker-library")
}

pacemaker {
    jvm()
    macos()
    android()
    ios()
    watchos()

    sourceSets {
        useNonAndroid()
        useJvmAndAndroid()
    }

    features {
        useAtomicFu()
    }
}
