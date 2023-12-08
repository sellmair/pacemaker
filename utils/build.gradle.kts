project.plugins.apply("kotlinx-atomicfu")
kotlin {
    sourceSets.commonMain.dependencies {
        implementation("org.jetbrains.kotlinx:atomicfu:0.22.0")
    }
}
