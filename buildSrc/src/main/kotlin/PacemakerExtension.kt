
import app.cash.sqldelight.gradle.SqlDelightExtension
import com.android.build.api.dsl.ApplicationExtension
import com.android.build.gradle.BaseExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType

class PacemakerExtension(
    private val project: Project,
    private val androidPluginId: String
) {

    private val kotlin = project.extensions.getByType(KotlinMultiplatformExtension::class.java)

    fun jvm() {
        kotlin {
            jvm()
            sourceSets.jvmTest.dependencies {
                implementation(kotlin("test-junit"))
            }
        }
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
                targetSdk = 35
            }

            if (this is ApplicationExtension) {
                packaging {
                    resources {
                        /* https://github.com/Kotlin/kotlinx-atomicfu/pull/344 */
                        excludes.add("META-INF/versions/9/previous-compilation-data.bin")
                    }
                }
            }
        }

        kotlin.apply {
            sourceSets.androidMain.dependencies {
                implementation(Dependencies.coroutinesAndroid)
                implementation(Dependencies.androidXCoreKtx)
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
        fun useSqlDelight(configure: SqlDelightExtension.() -> Unit) {
            project.plugins.apply("app.cash.sqldelight")
            kotlin {
                sourceSets.commonMain.dependencies {
                    implementation("app.cash.sqldelight:coroutines-extensions:2.0.2")
                }

                sourceSets.androidMain.dependencies {
                    implementation("app.cash.sqldelight:android-driver:2.0.2")
                }

                sourceSets.getByName("androidUnitTest").dependencies {
                    implementation("app.cash.sqldelight:sqlite-driver:2.0.2")
                }

                sourceSets.nativeMain.dependencies {
                    implementation("app.cash.sqldelight:native-driver:2.0.2")
                }


            }

            project.extensions.configure(SqlDelightExtension::class.java) {
                this.configure()
            }
        }

        fun useAtomicFu() {
            project.plugins.apply("org.jetbrains.kotlinx.atomicfu")
            kotlin {
                sourceSets.commonMain.dependencies {
                    implementation("org.jetbrains.kotlinx:atomicfu:0.26.1")
                }
            }
        }
    }

    operator fun <T> T.invoke(configure: T.() -> Unit) {
        configure(this)
    }
}
