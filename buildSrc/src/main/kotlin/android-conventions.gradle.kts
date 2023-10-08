import   com.android.build.gradle.BaseExtension
import com.android.build.gradle.api.AndroidBasePlugin

plugins.withType<AndroidBasePlugin>().configureEach {
    extensions.configure<BaseExtension> {
        compileSdkVersion(34)
        defaultConfig {
            minSdk = 31
        }
    }
}