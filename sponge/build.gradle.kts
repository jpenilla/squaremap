import org.spongepowered.gradle.plugin.config.PluginLoaders
import org.spongepowered.plugin.metadata.model.PluginDependency

plugins {
  `platform-conventions`
  id("org.spongepowered.gradle.plugin") version "2.0.0"
  id("org.spongepowered.gradle.vanilla")
}

val minecraftVersion = "1.18.1"

minecraft {
  version(minecraftVersion)
  accessWideners(project(":squaremap-common").layout.projectDirectory.file("src/main/resources/squaremap-common.accesswidener"))
}

dependencies {
  implementation(project(":squaremap-common")) {
    exclude("io.leangen.geantyref")
  }
  implementation("cloud.commandframework:cloud-sponge") {
    exclude("io.leangen.geantyref")
  }

  compileOnly("org.spongepowered:mixin:0.8.4")
}

sponge {
  apiVersion("9.0.0-SNAPSHOT")
  plugin("squaremap") {
    loader {
      name(PluginLoaders.JAVA_PLAIN)
      version("1.0")
    }
    license("MIT")
    entrypoint("xyz.jpenilla.squaremap.sponge.SquaremapSponge")
    dependency("spongeapi") {
      loadOrder(PluginDependency.LoadOrder.AFTER)
      optional(false)
    }
  }
}

tasks {
  shadowJar {
    archiveFileName.set("squaremap-sponge-mc$minecraftVersion-${project.version}.jar")
    listOf(
      "cloud.commandframework",
    ).forEach(::reloc)
    manifest {
      attributes(
        "Access-Widener" to "squaremap-common.accesswidener",
        "MixinConfigs" to "squaremap-sponge.mixins.json",
      )
    }
  }
}

squaremapPlatform {
  productionJar.set(tasks.shadowJar.flatMap { it.archiveFile })
}
