enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
  repositories {
    gradlePluginPortal()
    maven("https://repo.spongepowered.org/repository/maven-public/")
    maven("https://maven.fabricmc.net/")
    maven("https://maven.architectury.dev/")
  }
  includeBuild("build-logic")
}

plugins {
  id("org.gradle.toolchains.foojay-resolver-convention") version "0.7.0"
}

rootProject.name = "squaremap"

setupSubproject("api")
setupSubproject("common")
setupSubproject("paper")
setupSubproject("fabric")
// setupSubproject("forge")
setupSubproject("sponge")

fun setupSubproject(moduleName: String) {
  val name = "squaremap-$moduleName"
  include(name)
  val proj = project(":$name")
  proj.projectDir = file(moduleName)
}
