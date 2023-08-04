import org.spongepowered.gradle.plugin.config.PluginLoaders
import org.spongepowered.plugin.metadata.model.PluginDependency

plugins {
  id("platform-conventions")
  id("org.spongepowered.gradle.plugin")
  id("org.spongepowered.gradle.vanilla")
}

val minecraftVersion = libs.versions.minecraft

minecraft {
  version().set(minecraftVersion)
  accessWideners(project(":squaremap-common").layout.projectDirectory.file("src/main/resources/squaremap-common.accesswidener"))
}

dependencies {
  implementation(projects.squaremapCommon) {
    exclude("io.leangen.geantyref")
    exclude("com.google.inject")
  }
  implementation(libs.cloudSponge) {
    exclude("io.leangen.geantyref")
  }

  compileOnly(libs.mixin)
}

// https://github.com/SpongePowered/SpongeGradle/issues/70
configurations.spongeRuntime {
    resolutionStrategy {
        eachDependency {
            if (target.name == "spongevanilla") {
                useVersion("1.20.+")
            }
        }
    }
}

sponge {
  apiVersion("11.0.0-SNAPSHOT")
  plugin("squaremap") {
    loader {
      name(PluginLoaders.JAVA_PLAIN)
      version("1.0")
    }
    license("MIT")
    entrypoint("xyz.jpenilla.squaremap.sponge.SquaremapSpongeBootstrap")
    dependency("spongeapi") {
      loadOrder(PluginDependency.LoadOrder.AFTER)
      optional(false)
    }
  }
}

tasks {
  shadowJar {
    archiveFileName.set(productionJarName(minecraftVersion))
    listOf(
      "cloud.commandframework",
      "com.google.inject.assistedinject",
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

modrinth {
  gameVersions.add(minecraftVersion)
}
