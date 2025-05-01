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
    minimumToolchain(21)
    target(21)
  }
}

repositories {
  mavenCentral()
  maven("https://repo.jpenilla.xyz/snapshots/") {
    mavenContent {
      includeModule("org.incendo", "cloud-sponge")
      includeGroup("xyz.jpenilla")
      snapshotsOnly()
    }
  }
  sonatype.s01Snapshots()
  sonatype.ossSnapshots()
  maven("https://repo.papermc.io/repository/maven-public/")
  maven("https://cursemaven.com") {
    content {
      includeGroup("curse.maven")
    }
  }
}

tasks.withType(JavaCompile::class).configureEach {
  // We don't care about annotations being unclaimed by processors,
  // missing annotation (values), or Java serialization
  options.compilerArgs.add("-Xlint:-processing,-classfile,-serial")
}
