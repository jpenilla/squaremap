enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
  repositories {
    gradlePluginPortal()
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.spongepowered.org/repository/maven-public/")
    maven("https://maven.fabricmc.net/")
    maven("https://maven.neoforged.net/releases/")
    maven("https://maven.architectury.dev/")
    maven("https://repo.jpenilla.xyz/snapshots/")
  }
  includeBuild("build-logic")
}

buildscript {
  configurations.all {
    resolutionStrategy {
      eachDependency {
        if (requested.group == "com.google.code.gson" && requested.name == "gson") {
          useVersion("2.10.1")
          because("project plugins need newer version than foojay-resolver-convention")
        }
      }
    }
  }
}

plugins {
  id("org.gradle.toolchains.foojay-resolver-convention") version "0.7.0"
}

rootProject.name = "squaremap"

setupSubproject("api")
setupSubproject("common")
//setupSubproject("paper")
//setupSubproject("fabric")
setupSubproject("neoforge")
setupSubproject("sponge")

fun setupSubproject(moduleName: String) {
  val name = "squaremap-$moduleName"
  include(name)
  val proj = project(":$name")
  proj.projectDir = file(moduleName)
}
