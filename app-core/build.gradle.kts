plugins {
    id("pacemaker-library")
    id("app.cash.sqldelight")
}

pacemaker {
    ios()
    android()
    jvm()

    features {
        useSqlDelight {
            databases {
                create("PacemakerDatabase") {
                    srcDirs(file("src/sql"))
                    packageName = "io.sellmair.pacemaker.sql"
                }
            }
        }
    }
}

kotlin {
    sourceSets.commonMain.dependencies {
        api(project(":models"))
        api(project(":utils"))
        api(project(":bluetooth"))
        implementation(Dependencies.multiplatform_settings)
    }
}
