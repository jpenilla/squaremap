import net.kyori.indra.IndraExtension

plugins {
    val indraVersion = "2.0.6"
    id("net.kyori.indra") version indraVersion apply false
    id("net.kyori.indra.publishing") version indraVersion apply false
    id("net.kyori.indra.git") version indraVersion
}

allprojects {
    group = "xyz.jpenilla"
    version = "1.1.0-SNAPSHOT"
    description = "Minimalistic and lightweight world map viewer for Paper servers"
}

subprojects {
    apply(plugin = "java-library")
    apply(plugin = "net.kyori.indra")
    apply(plugin = "net.kyori.indra.git")

    if (name.endsWith("-plugin")) {
        version = (version as String).decorateVersion()
    }

    configure<IndraExtension> {
        javaVersions {
            minimumToolchain(17)
            target(17)
        }

        configurePublications {
            pom {
                developers {
                    developer {
                        id.set("jmp")
                        timezone.set("America/Los Angeles")
                    }
                }
            }
        }
    }

    repositories {
        mavenCentral()
        maven("https://papermc.io/repo/repository/maven-public/")
        maven("https://maven.fabricmc.net/") {
            mavenContent { includeGroup("net.fabricmc") }
        }
        maven("https://repo.incendo.org/content/repositories/snapshots/") {
            mavenContent {
                includeGroup("cloud.commandframework")
                snapshotsOnly()
            }
        }
        maven("https://repo.jpenilla.xyz/snapshots/") {
            mavenContent {
                includeGroup("xyz.jpenilla")
                snapshotsOnly()
            }
        }
    }
}

fun lastCommitHash(): String = indraGit.commit()?.name?.substring(0, 7)
    ?: error("Could not determine commit hash")

fun String.decorateVersion(): String = if (endsWith("-SNAPSHOT")) "$this+${lastCommitHash()}" else this
