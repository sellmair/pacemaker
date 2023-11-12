import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    id("org.jetbrains.kotlin.multiplatform")
}


kotlin {
    jvmToolchain(17)

    sourceSets.all {
        languageSettings.enableLanguageFeature("ContextReceivers")
    }

    sourceSets.commonMain.dependencies {
        if (project.name != "utils") {
            implementation(project(":utils"))
        }

        implementation(Dependencies.coroutinesCore)
        implementation(Dependencies.okio)
        implementation(Dependencies.kotlinxDatetime)
    }

    sourceSets.commonTest.dependencies {
        implementation(kotlin("test"))
        implementation(kotlin("test-common"))
        implementation(kotlin("test-annotations-common"))
        implementation(Dependencies.coroutinesTest)
        implementation(Dependencies.coroutinesDebug)
    }

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    kotlin.applyDefaultHierarchyTemplate {
        common {
            group("phone") {
                withAndroidTarget()
                group("ios")
            }

            group("apple") {
                group("iosAndMac") {
                    group("ios")
                    group("macos")
                }
            }
        }
    }

    //https://youtrack.jetbrains.com/issue/KT-61573
    targets.all {
        compilations.all {
            compilerOptions.options.freeCompilerArgs.add("-Xexpect-actual-classes")
        }
    }
}
