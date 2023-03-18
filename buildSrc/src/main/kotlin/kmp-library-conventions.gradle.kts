import com.android.build.gradle.api.AndroidBasePlugin

plugins {
    id("kmp-conventions")
    id("android-conventions")
}

kotlin {
    afterEvaluate {
        if (!plugins.hasPlugin(AndroidBasePlugin::class)) {
            jvm()
        }
    }
}
