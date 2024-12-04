plugins {
  `kotlin-dsl`
}

repositories {
  gradlePluginPortal()
  mavenCentral()
  maven("https://repo.papermc.io/repository/maven-public/")
  maven("https://maven.fabricmc.net/")
  maven("https://maven.neoforged.net/releases/")
  maven("https://maven.architectury.dev/")
  maven("https://repo.jpenilla.xyz/snapshots/")
}

dependencies {
  implementation(libs.mdg)
  implementation(libs.indraCommon)
  implementation(libs.indraPublishingSonatype)
  implementation(libs.shadow)
  implementation(libs.mod.publish.plugin)
  implementation(libs.loom)
  implementation(libs.paperweightUserdev)
}
