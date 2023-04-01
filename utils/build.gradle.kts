@file:Suppress("OPT_IN_USAGE")

import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType

plugins {
    id("kmp-library-conventions")
    id("com.android.library")
}

android {
    namespace = "io.sellmair.pacemaker.utils"
}

kotlin.jvm()

kotlin.targetHierarchy.custom {
    common {
        group("nonAndroid") {
            withCompilations { true }
            withoutCompilations { it.target.platformType == KotlinPlatformType.androidJvm }
        }

        group("jvmAndAndroid") {
            withAndroid()
            withJvm()
        }
    }
}

kotlin.sourceSets.commonMain.get().dependencies {
    api(Dependencies.coroutinesCore)
    implementation(Dependencies.okio)
}