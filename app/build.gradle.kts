import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    `pacemaker-library`
    org.jetbrains.compose
    org.jetbrains.kotlin.plugin.compose
    id("org.jetbrains.compose.hot-reload")
}

pacemaker {
    ios()
    android()
    jvm()
}

kotlin {
    compilerOptions {
        optIn.add("org.jetbrains.compose.resources.ExperimentalResourceApi")
    }

    sourceSets.commonMain.get().dependencies {
        implementation(project(":app-core"))

        implementation(deps.evas.compose)

        /* COMPOSE */
        implementation(deps.compose.ui)
        implementation(deps.compose.foundation)
        implementation(deps.compose.runtime)
        implementation(deps.compose.resources)
        implementation(deps.compose.ui.tooling.preview)

        implementation(deps.compose.material3)
        implementation(deps.compose.material.icons.extended)

    }

    sourceSets.jvmMain.dependencies {
        implementation(compose.desktop.currentOs)
        implementation(deps.coroutines.swing)
    }

    sourceSets.androidMain.get().dependencies {
        /* androidx */
        implementation(deps.androidx.activity.compose)
        implementation(deps.compose.ui.tooling.preview)
    }

}

kotlin.targets.withType<KotlinNativeTarget>().configureEach {
    binaries.framework {
        baseName = "LibPacemaker"
        isStatic = true
        binaryOption("bundleId", "io.sellmair.pacemaker.LibPacemaker")
    }
}
