plugins {
    id("pacemaker-library")
}

pacemaker {
    jvm()
    ios()
    macos()

    features {
        kotlinxSerialisation()
    }
}
