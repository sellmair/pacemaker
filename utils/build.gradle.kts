plugins {
    id("pacemaker-library")
}

pacemaker {
    jvm()
    macos()
    android()
    ios()

    sourceSets {
        useNonAndroid()
        useJvmAndAndroid()
    }

    features {
        useAtomicFu()
    }
}
