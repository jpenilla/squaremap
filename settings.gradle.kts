enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
  repositories {
    gradlePluginPortal()
    maven("https://papermc.io/repo/repository/maven-public/")
    maven("https://repo.spongepowered.org/repository/maven-public/")
    maven("https://maven.fabricmc.net/")
    maven("https://maven.quiltmc.org/repository/release/")
    maven("https://repo.jpenilla.xyz/snapshots/")
  }
  includeBuild("build-logic")
}

buildscript {
  configurations.all {
    resolutionStrategy {
      eachDependency {
        if (requested.group == "com.google.code.gson" && requested.name == "gson") {
          useVersion("2.8.9")
          because("loom needs 2.8.9, polyglot version catalogs adds 2.8.0 to classpath, force 2.8.9 to avoid issues")
        }
      }
    }
  }
}

plugins {
  id("ca.stellardrift.polyglot-version-catalogs") version "5.0.0"
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
