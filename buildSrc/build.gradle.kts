plugins {
  `kotlin-dsl`
}

repositories {
  gradlePluginPortal()
  maven("https://repo.spongepowered.org/repository/maven-public/")
}

dependencies {
  implementation("org.spongepowered:vanillagradle:0.2.1-SNAPSHOT")
  implementation("net.kyori:indra-common:2.0.6")
  implementation("gradle.plugin.com.github.johnrengelman:shadow:7.1.2")
}
