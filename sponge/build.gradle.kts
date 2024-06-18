import org.spongepowered.gradle.plugin.config.PluginLoaders
import org.spongepowered.plugin.metadata.model.PluginDependency

plugins {
  id("squaremap.platform")
  alias(libs.plugins.sponge.gradle.plugin)
  id("org.spongepowered.gradle.vanilla")
  alias(libs.plugins.sponge.gradle.ore)
}

val minecraftVersion = libs.versions.minecraft

minecraft {
  version().set(minecraftVersion)
  accessWideners(project(":squaremap-common").layout.projectDirectory.file("src/main/resources/squaremap-common.accesswidener"))
}

dependencies {
  implementation(projects.squaremapCommon) {
    exclude("io.leangen.geantyref")
  }
  implementation(libs.cloudSponge) {
    exclude("io.leangen.geantyref")
  }
  compileOnly("javax.inject:javax.inject:1")

  compileOnly(libs.mixin)
}

// https://github.com/SpongePowered/SpongeGradle/issues/70
/*
configurations.spongeRuntime {
    resolutionStrategy {
        eachDependency {
            if (target.name == "spongevanilla") {
                useVersion("1.20.+")
            }
        }
    }
}
 */

sponge {
  apiVersion("12.0.0-SNAPSHOT")
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
      "org.incendo.cloud",
      "com.google.inject",
      "jakarta.inject",
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
