plugins {
  `kotlin-dsl`
}

repositories {
  gradlePluginPortal()
}

dependencies {
  implementation("net.kyori:indra-common:2.0.6")
  implementation("gradle.plugin.com.github.johnrengelman:shadow:7.1.2")
}
