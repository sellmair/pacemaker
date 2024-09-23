import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    id("org.jetbrains.kotlin.multiplatform")
}


kotlin {
    jvmToolchain(17)

    sourceSets.commonMain.dependencies {
        if (project.name != "utils") {
            implementation(project(":utils"))
        }

        implementation(Dependencies.coroutinesCore)
        implementation(Dependencies.okio)
        implementation(Dependencies.kotlinxDatetime)
        implementation(Dependencies.kotlinxImmutable)
        implementation(Dependencies.composeRuntime(project))
        implementation(Dependencies.evas)
    }

    sourceSets.commonTest.dependencies {
        implementation(kotlin("test"))
        implementation(kotlin("test-common"))
        implementation(kotlin("test-annotations-common"))
        implementation(Dependencies.coroutinesTest)
        implementation(Dependencies.coroutinesDebug)
    }


    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
}

tasks.withType<Test>().configureEach {
    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = true
        showExceptions = true
        showStackTraces = true
        showCauses = true
        outputs.upToDateWhen { false }
    }
}