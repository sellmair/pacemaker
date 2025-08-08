
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    id("pacemaker-application")
}

pacemaker {
    macos()
}

kotlin {
    sourceSets.getByName("commonMain").dependencies {
        implementation(project(":bluetooth"))
        implementation(Dependencies.coroutines_core)
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
