plugins {
    `java-library`
    val indraVersion = "2.0.6"
    id("net.kyori.indra") version indraVersion
    id("net.kyori.indra.git") version indraVersion
}

allprojects {
    group = "xyz.jpenilla"
    version = "1.1.0-SNAPSHOT".decorateVersion()
    description = "Minimalistic and lightweight world map viewer for Paper servers"
}

subprojects {
    apply(plugin = "java-library")
    apply(plugin = "net.kyori.indra")
    apply(plugin = "net.kyori.indra.git")

    indra {
        javaVersions {
            target(17)
        }
    }

    repositories {
        mavenCentral()
        maven("https://papermc.io/repo/repository/maven-public/")
        maven("https://repo.incendo.org/content/repositories/snapshots/")
        maven("https://repo.codemc.org/repository/maven-public/")
    }
}

fun lastCommitHash(): String = indraGit.commit()?.name?.substring(0, 7)
    ?: error("Could not determine commit hash")

fun String.decorateVersion(): String = if (endsWith("-SNAPSHOT")) "$this+${lastCommitHash()}" else this
