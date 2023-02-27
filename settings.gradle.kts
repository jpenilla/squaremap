enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
  repositories {
    gradlePluginPortal()
    maven("https://repo.spongepowered.org/repository/maven-public/")
    maven("https://maven.fabricmc.net/")
    maven("https://maven.quiltmc.org/repository/release/")
    maven("https://repo.jpenilla.xyz/snapshots/")
  }
  includeBuild("build-logic")
}

plugins {
  id("org.gradle.toolchains.foojay-resolver-convention") version "0.4.0"
  id("ca.stellardrift.polyglot-version-catalogs") version "6.0.1"
}

rootProject.name = "squaremap"

setupSubproject("squaremap-api") {
  projectDir = file("api")
}
setupSubproject("squaremap-common") {
  projectDir = file("common")
}
setupSubproject("squaremap-paper") {
  projectDir = file("paper")
}
setupSubproject("squaremap-fabric") {
  projectDir = file("fabric")
}
setupSubproject("squaremap-sponge") {
  projectDir = file("sponge")
}

inline fun setupSubproject(name: String, block: ProjectDescriptor.() -> Unit) {
  include(name)
  project(":$name").apply(block)
}
