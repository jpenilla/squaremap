plugins {
    id("net.kyori.indra.publishing")
}

dependencies {
    compileOnly("io.papermc.paper", "paper-api", "1.18-R0.1-SNAPSHOT")
    compileOnlyApi("org.checkerframework", "checker-qual", "3.19.0")
}

java {
    withJavadocJar()
    withSourcesJar()
}

indra {
    publishSnapshotsTo("jmp", "https://repo.jpenilla.xyz/snapshots")
}
