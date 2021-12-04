plugins {
    `maven-publish`
}

dependencies {
    compileOnly("io.papermc.paper", "paper-api", "1.18-R0.1-SNAPSHOT")
    compileOnlyApi("org.checkerframework", "checker-qual", "3.19.0")
}

java {
    withJavadocJar()
    withSourcesJar()
}

publishing {
    publications {
        register<MavenPublication>("maven") {
            from(components["java"])
        }
    }

    repositories.maven("https://repo.jpenilla.xyz/snapshots") {
        credentials(PasswordCredentials::class)
    }
}
