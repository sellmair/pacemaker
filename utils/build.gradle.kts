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
kotlin.androidTarget()

kotlin.targetHierarchy.custom {
    common {
        group("nonAndroid") {
            withCompilations { true }
            excludeCompilations { it.target.platformType == KotlinPlatformType.androidJvm }
        }

        group("jvmAndAndroid") {
            withAndroidTarget()
            withJvm()
        }
    }
}

kotlin.sourceSets.commonMain.get().dependencies {
    api(Dependencies.coroutinesCore)
    implementation(Dependencies.okio)
}