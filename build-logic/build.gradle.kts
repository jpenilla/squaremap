plugins {
  `kotlin-dsl`
}

repositories {
  gradlePluginPortal()
  maven("https://repo.spongepowered.org/repository/maven-public/")
  maven("https://maven.fabricmc.net/")
  maven("https://maven.architectury.dev/")
}

dependencies {
  implementation(libs.vanillaGradle)
  implementation(libs.indraCommon)
  implementation(libs.indraPublishingSonatype)
  implementation(libs.shadow)
  implementation(libs.minotaur)
  implementation(libs.loom)
  implementation(libs.paperweightUserdev)
}
