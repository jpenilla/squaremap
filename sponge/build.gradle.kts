import org.spongepowered.gradle.plugin.config.PluginLoaders
import org.spongepowered.plugin.metadata.model.PluginDependency

plugins {
  id("squaremap.platform.mdg")
  alias(libs.plugins.sponge.gradle.plugin)
  alias(libs.plugins.sponge.gradle.ore)
}

val minecraftVersion = libs.versions.minecraft

neoForge {
  neoFormVersion = libs.versions.neoform
}

dependencies {
  shade(projects.squaremapCommon) {
    exclude("io.leangen.geantyref")
  }
  shade(libs.cloudSponge) {
    exclude("io.leangen.geantyref")
  }
  compileOnly("javax.inject:javax.inject:1")

  compileOnly(libs.mixin)
}

// https://github.com/SpongePowered/SpongeGradle/issues/70
configurations.spongeRuntime {
    resolutionStrategy {
        eachDependency {
            if (target.name == "spongevanilla") {
                useVersion("1.21.4-13.+")
            }
        }
    }
}

sponge {
  apiVersion("13.0.0-SNAPSHOT")
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
  productionJar {
    archiveFileName.set(productionJarName(minecraftVersion))
  }
  shadowJar {
    listOf(
      "org.incendo.cloud",
    ).forEach(::reloc)
    manifest {
      attributes(
        "Access-Widener" to "squaremap-sponge.accesswidener",
        "MixinConfigs" to "squaremap-sponge.mixins.json",
      )
    }
  }
}

oreDeployment {
  defaultPublication {
    versionBody.set(releaseNotes)
    publishArtifacts.setFrom(squaremapPlatform.productionJar)
  }
}

publishMods.modrinth {
  minecraftVersions.add(minecraftVersion)
  modLoaders.add("sponge")
}
