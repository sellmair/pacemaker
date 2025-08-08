import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `kotlin-dsl`
}

dependencies {
    implementation(deps.compose.gradlePlugin)
    implementation(deps.kotlin.composeCompilerPlugin)
    implementation(deps.kotlin.gradlePlugin)
    implementation(deps.android.gradlePlugin)
    implementation(deps.sqldelight.gradlePlugin)
    implementation(deps.atomicFu.gradlePlugin)
}


/*
Make dependencies from the version catalog available in convention scripts and buildSrc!
 */
run {
    tasks.register("generateDependencies") {
        val output = file("src/main/kotlin/Dependencies.kt")
        outputs.file(output)

        val catalog = versionCatalogs.named("deps")
        val libraries = catalog.libraryAliases.associateWith { alias ->
            catalog.findLibrary(alias).get().get().toString()
        }

        inputs.property("libraries", libraries)

        doFirst {
            output.parentFile.mkdirs()
            output.outputStream().bufferedWriter().use { writer ->
                writer.appendLine("@Suppress(\"unused\")")
                writer.appendLine("object Dependencies {")
                libraries.forEach { (alias, notation) ->
                    val escapedAlias = alias.replace(".", "_")
                    writer.appendLine("    const val $escapedAlias = \"$notation\"")
                }
                writer.appendLine("}")
            }
        }
    }

    tasks.withType<KotlinCompile>().configureEach {
        dependsOn("generateDependencies")
    }
}
