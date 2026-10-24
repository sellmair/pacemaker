import app.cash.sqldelight.gradle.SqlDelightExtension
import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType

class PacemakerExtension(
    private val project: Project
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
        kotlin.iosArm64()
        kotlin.iosSimulatorArm64()
    }

    fun macos() {
        kotlin.macosArm64()
    }

    fun android() {
        project.plugins.apply("com.android.kotlin.multiplatform.library")
        val androidTarget = kotlin.targets.getByName("android") as KotlinMultiplatformAndroidLibraryTarget
        androidTarget.apply {
            namespace = "io.sellmair.${project.name.replace("-", ".")}"
            compileSdk = 37
            minSdk = 31
            androidResources {
                enable = true
            }
            withHostTest {}
            kotlin.sourceSets.getByName("androidHostTest").dependencies {
                implementation("org.jetbrains.kotlin:kotlin-test-junit")
            }
        }

        kotlin.apply {
            sourceSets.androidMain.dependencies {
                implementation(Dependencies.coroutines_android)
                implementation(Dependencies.androidx_core_ktx)
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
                    implementation(Dependencies.sqldelight_coroutineExtensions)
                }

                sourceSets.androidMain.dependencies {
                    implementation(Dependencies.sqldelight_androidDriver)
                }

                sourceSets.getByName("androidHostTest").dependencies {
                    implementation(Dependencies.sqldelight_sqliteDriver)
                }

                sourceSets.nativeMain.dependencies {
                    implementation(Dependencies.sqldelight_nativeDriver)
                }

                sourceSets.jvmMain.dependencies {
                    implementation(Dependencies.sqldelight_sqliteDriver)
                }
            }

            project.extensions.configure(SqlDelightExtension::class.java) {
                this.configure()
            }
        }
    }

    operator fun <T> T.invoke(configure: T.() -> Unit) {
        configure(this)
    }
}
