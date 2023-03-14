@file:Suppress("OPT_IN_USAGE")

import com.android.build.gradle.api.AndroidBasePlugin
import org.gradle.kotlin.dsl.kotlin
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.konan.target.Family

plugins {
    kotlin("multiplatform")
    id("android-conventions")
}

kotlin {
    plugins.withType<AndroidBasePlugin>().configureEach {
        android()
    }

    iosX64()
    iosArm64()
    iosSimulatorArm64()

    val commonMain by sourceSets.getting
    val mobileMain by sourceSets.creating
    val nativeMain by sourceSets.creating
    val appleMain by sourceSets.creating
    val macosMain by sourceSets.creating
    val iosMain by sourceSets.creating

    nativeMain.dependsOn(commonMain)
    appleMain.dependsOn(nativeMain)
    iosMain.dependsOn(appleMain)
    macosMain.dependsOn(appleMain)
    macosMain.dependsOn(nativeMain)
    iosMain.dependsOn(mobileMain)
    mobileMain.dependsOn(commonMain)

    targets.withType<KotlinNativeTarget>().all {
        compilations.getByName("main").kotlinSourceSets.forAll { sourceSet ->
            when (konanTarget.family) {
                Family.OSX -> sourceSet.dependsOn(macosMain)
                Family.IOS -> sourceSet.dependsOn(iosMain)
                else -> sourceSet.dependsOn(nativeMain)
            }
        }
    }

    sourceSets.matching { it.name == "androidMain" }.all {
        dependsOn(mobileMain)
    }

    /* Waiting for 1.8.20
    targetHierarchy.default {
        common {
            group("mobile") {
                withAndroid()
                withJvm()
                withIos()
            }
        }
    }
     */

    sourceSets.all {
        languageSettings.optIn("kotlin.time.ExperimentalTime")
    }

    jvmToolchain(8)
}