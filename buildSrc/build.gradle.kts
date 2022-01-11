plugins {
  `kotlin-dsl`
}

repositories {
  gradlePluginPortal()
  maven("https://repo.spongepowered.org/repository/maven-public/")
}

dependencies {
  implementation(libs.vanillaGradle)
  implementation(libs.indraCommon)
  implementation(libs.shadow)
}
