plugins {
    id("net.kyori.indra.publishing")
}

java {
    disableAutoTargetJvm()
}

dependencies {
    compileOnly("io.papermc.paper", "paper-api", "1.18.1-R0.1-SNAPSHOT")
    compileOnlyApi("org.checkerframework", "checker-qual", "3.21.0")
}

indra {
    javaVersions {
        target(8)
    }

    publishSnapshotsTo("jmp", "https://repo.jpenilla.xyz/snapshots/")
}

java {
    withJavadocJar()
    withSourcesJar()
}
