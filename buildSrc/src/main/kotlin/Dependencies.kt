import org.gradle.api.Project
import org.jetbrains.compose.ComposePlugin

object Dependencies {
    const val coroutinesCore = "org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1"
    const val coroutinesAndroid = "org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.1"
    const val coroutinesTest = "org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.1"
    const val okio = "com.squareup.okio:okio:3.9.0"
    const val kotlinxDatetime = "org.jetbrains.kotlinx:kotlinx-datetime:0.6.1"
    const val kotlinxImmutable = "org.jetbrains.kotlinx:kotlinx-collections-immutable:0.3.8"
    const val multiplatformSettings = "com.russhwolf:multiplatform-settings:1.3.0"
    const val androidXCoreKtx = "androidx.core:core-ktx:1.13.1"
    const val evas = "io.sellmair:evas:1.2.0"
    const val evasCompose = "io.sellmair:evas-compose:1.2.0"
    fun composeRuntime(project: Project) = ComposePlugin.Dependencies(project).runtime
}
