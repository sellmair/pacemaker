import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSetTree

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

kotlin {
    androidTarget {
        @Suppress("OPT_IN_USAGE")
        instrumentedTestVariant {
            sourceSetTree = KotlinSourceSetTree.test
        }
    }
}