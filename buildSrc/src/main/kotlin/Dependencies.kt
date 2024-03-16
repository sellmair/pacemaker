import org.gradle.api.Project
import org.jetbrains.compose.ComposePlugin

object Dependencies {
    const val coroutinesCore = "org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0"
    const val coroutinesAndroid = "org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.0"
    const val coroutinesDebug = "org.jetbrains.kotlinx:kotlinx-coroutines-debug:1.8.0"
    const val coroutinesTest = "org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0"
    const val okio = "com.squareup.okio:okio:3.6.0"
    const val kotlinxDatetime = "org.jetbrains.kotlinx:kotlinx-datetime:0.4.1"
    const val kotlinxImmutable = "org.jetbrains.kotlinx:kotlinx-collections-immutable:0.3.6"
    const val multiplatformSettings = "com.russhwolf:multiplatform-settings:1.1.0"
    const val androidXCoreKtx = "androidx.core:core-ktx:1.12.0"
    fun composeRuntime(project: Project) = ComposePlugin.Dependencies(project).runtime
}
