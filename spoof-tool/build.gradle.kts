@file:Suppress("OPT_IN_USAGE")

import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    id("kmp-conventions")
}

kotlin {
    sourceSets.getByName("commonMain").dependencies {
        implementation(project(":bluetooth"))
        implementation(Dependencies.coroutinesCore)
        implementation(Dependencies.okio)
    }

    targets.withType<KotlinNativeTarget>().all {
        binaries.executable {
            entryPoint("io.sellmair.pacemaker.spoof.main")
            runTaskProvider?.configure {
                this.workingDir(projectDir)
                this.standardInput = System.`in`
            }
        }
    }
}