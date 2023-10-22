plugins {
    id("pacemaker-library")
    id("app.cash.sqldelight")
}

pacemaker {
    ios()
    android()

    features {
        useSqlDelight {
            databases {
                create("PacemakerDatabase") {
                    srcDirs(file("src/sql"))
                    packageName = "io.sellmair.pacemaker.sql"
                    generateAsync = true
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
    }

    sourceSets.androidMain.dependencies {
        implementation("androidx.core:core-ktx:1.12.0")
    }
}

