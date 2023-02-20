@file:Suppress("OPT_IN_USAGE")

import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    kotlin("multiplatform")
}

kotlin {
    macosArm64()
    macosX64()
    targetHierarchy.default()

    sourceSets.getByName("commonMain").dependencies {
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
        implementation("com.squareup.okio:okio:3.3.0")
    }

    targets.withType<KotlinNativeTarget>().all {
        binaries.executable {
            entryPoint("io.sellmair.broadheart.spoof.main")
            runTaskProvider?.configure {
                this.workingDir(projectDir)
            }
        }
    }
}