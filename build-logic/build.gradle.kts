plugins {
  `kotlin-dsl`
}

repositories {
  gradlePluginPortal()
  maven("https://repo.spongepowered.org/repository/maven-public/")
  maven("https://maven.fabricmc.net/")
  maven("https://maven.architectury.dev/")
  maven("https://maven.neoforged.net/releases/")
}

dependencies {
  implementation(libs.vanillaGradle)
  implementation(libs.indraCommon)
  implementation(libs.indraPublishingSonatype)
  implementation(libs.shadow)
  implementation(libs.mod.publish.plugin)
  implementation(libs.loom)
  implementation(libs.paperweightUserdev)
}
