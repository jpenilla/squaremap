enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
  repositories {
    gradlePluginPortal()
    mavenCentral()
    maven("https://maven.fabricmc.net/")
    maven("https://maven.neoforged.net/releases/")
    maven("https://maven.architectury.dev/")
    maven("https://repo.jpenilla.xyz/snapshots/")
  }
  includeBuild("build-logic")
}

plugins {
  id("org.gradle.toolchains.foojay-resolver-convention") version "0.9.0"
}

rootProject.name = "squaremap"

setupSubproject("api")
setupSubproject("common")
setupSubproject("paper")
include(":squaremap-paper:folia")
setupSubproject("fabric")
setupSubproject("neoforge")
setupSubproject("sponge")

fun setupSubproject(moduleName: String) {
  val name = "squaremap-$moduleName"
  include(name)
  val proj = project(":$name")
  proj.projectDir = file(moduleName)
}
