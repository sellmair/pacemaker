
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.gradle.api.tasks.JavaExec

plugins {
    id("pacemaker-application")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.compose.hot-reload")
}

pacemaker {
    macos()
    jvm()
}

kotlin {
    sourceSets.getByName("commonMain").dependencies {
        implementation(project(":bluetooth"))
    }

    sourceSets.getByName("jvmMain").dependencies {
        implementation(compose.desktop.currentOs)
        implementation(Dependencies.compose_ui)
        implementation(Dependencies.compose_foundation)
        implementation(Dependencies.compose_material3)
        implementation(Dependencies.evas_compose)
        implementation(Dependencies.coroutines_swing)
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

compose.desktop {
    application {
        mainClass = "io.sellmair.pacemaker.spoof.MainKt"
    }
}

val spoofToolProjectDir = project.projectDir
val nativeSpoofExecutable = layout.buildDirectory.file("bin/macosArm64/debugExecutable/spoof-tool.kexe")

tasks.withType<JavaExec>().configureEach {
    workingDir(spoofToolProjectDir)
    systemProperty("pacemaker.spoof.executable", nativeSpoofExecutable.get().asFile.absolutePath)
}

tasks.matching { it.name == "hotDevJvm" || it.name == "hotRunJvm" || it.name == "run" }.configureEach {
    dependsOn("linkDebugExecutableMacosArm64")
}
