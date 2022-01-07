import net.kyori.indra.IndraExtension

plugins {
  id("java-library")
  id("net.kyori.indra")
  id("net.kyori.indra.git")
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
  maven("https://oss.sonatype.org/content/repositories/snapshots/") {
    mavenContent { snapshotsOnly() }
  }
  maven("https://papermc.io/repo/repository/maven-public/")
  maven("https://maven.fabricmc.net/") {
    mavenContent { includeGroup("net.fabricmc") }
  }
  /*
  maven("https://repo.incendo.org/content/repositories/snapshots/") {
    mavenContent {
      includeGroup("cloud.commandframework")
      snapshotsOnly()
    }
  }
   */
  maven("https://repo.jpenilla.xyz/snapshots/") {
    mavenContent {
      includeGroup("cloud.commandframework")
      includeGroup("xyz.jpenilla")
      snapshotsOnly()
    }
  }
}
