import net.kyori.indra.IndraExtension

plugins {
  id("java-library")
  id("net.kyori.indra")
  id("net.kyori.indra.git")
}

group = rootProject.group
version = rootProject.version
description = rootProject.description

configure<IndraExtension> {
  javaVersions {
    minimumToolchain(17)
    target(17)
  }
}

repositories {
  mavenCentral()
  maven("https://repo.jpenilla.xyz/snapshots/") {
    mavenContent {
      includeGroup("cloud.commandframework")
      includeGroup("xyz.jpenilla")
      snapshotsOnly()
    }
  }
  maven("https://s01.oss.sonatype.org/content/repositories/snapshots/") {
    mavenContent { snapshotsOnly() }
  }
  maven("https://oss.sonatype.org/content/repositories/snapshots/") {
    mavenContent { snapshotsOnly() }
  }
  maven("https://repo.papermc.io/repository/maven-public/")
  maven("https://maven.fabricmc.net/") {
    mavenContent { includeGroup("net.fabricmc") }
  }
}
