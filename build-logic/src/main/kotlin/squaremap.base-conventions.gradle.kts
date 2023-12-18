plugins {
  id("java-library")
  id("net.kyori.indra")
  id("net.kyori.indra.git")
}

group = rootProject.group
version = rootProject.version
description = rootProject.description

indra {
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
  sonatype.s01Snapshots()
  sonatype.ossSnapshots()
  maven("https://repo.papermc.io/repository/maven-public/")
}

tasks.withType(JavaCompile::class).configureEach {
  // We don't care about annotations being unclaimed by processors
  options.compilerArgs.add("-Xlint:-processing")
}
