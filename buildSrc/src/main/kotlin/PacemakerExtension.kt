import com.android.build.gradle.BaseExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlinx.serialization.gradle.SerializationGradleSubplugin

class PacemakerExtension(
    private val project: Project,
    private val androidPluginId: String
) {

    private val kotlin = project.extensions.getByType(KotlinMultiplatformExtension::class.java)

    fun jvm() {
        kotlin.jvm()
    }

    fun ios() {
        kotlin.iosX64()
        kotlin.iosArm64()
        kotlin.iosSimulatorArm64()
    }

    fun macos() {
        kotlin.macosArm64()
        kotlin.macosX64()
    }

    fun android() {
        project.plugins.apply(androidPluginId)
        kotlin.androidTarget()
        project.extensions.configure(BaseExtension::class) {
            compileSdkVersion(34)
            namespace = "io.sellmair.${project.name.replace("-", ".")}"
            defaultConfig {
                minSdk = 31
            }
        }
    }

    val sourceSets = SourceSetsDsl()

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    inner class SourceSetsDsl {
        fun useNonAndroid() {
            kotlin.applyDefaultHierarchyTemplate {
                common {
                    group("nonAndroid") {
                        withCompilations { true }
                        excludeCompilations { it.target.platformType == KotlinPlatformType.androidJvm }
                    }
                }
            }
        }

        fun useJvmAndAndroid() {
            kotlin.applyDefaultHierarchyTemplate {
                common {
                    group("jvmAndAndroid") {
                        withAndroidTarget()
                        withJvm()
                    }
                }
            }
        }
    }


    val features = FeaturesDsl()

    fun features(configure: FeaturesDsl.() -> Unit) {
        features.configure()
    }

    inner class FeaturesDsl {
        fun kotlinxSerialisation() {
            project.plugins.apply(SerializationGradleSubplugin::class.java)
            kotlin.apply {
                sourceSets.commonMain.dependencies {
                    api(Dependencies.kotlinxSerializationJson)
                }
            }
        }
    }

    operator fun <T> T.invoke(configure: T.() -> Unit) {
        configure(this)
    }
}
