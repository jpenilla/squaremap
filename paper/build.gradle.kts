import io.papermc.paperweight.userdev.ReobfArtifactConfiguration
import xyz.jpenilla.resourcefactory.bukkit.BukkitPluginYaml
import xyz.jpenilla.runpaper.task.RunServer

plugins {
  id("squaremap.platform")
  id("io.papermc.paperweight.userdev")
  alias(libs.plugins.run.paper)
  alias(libs.plugins.hangar.publish)
  alias(libs.plugins.resource.factory.bukkit)
}

val minecraftVersion = libs.versions.minecraft
val plainMinecraftVersion = minecraftVersion.get()
  .split("[.-]".toRegex())
  .mapNotNull { s -> s.toIntOrNull() }
  .joinToString(".")

dependencies {
  paperweight.paperDevBundle("${minecraftVersion.get()}-R0.1-SNAPSHOT")

  implementation(projects.squaremapCommon)
  implementation(projects.squaremapPaper.folia)

  implementation(libs.cloudPaper)
  implementation(libs.bStatsBukkit)
}

tasks {
  jar {
    manifest {
      attributes("squaremap-target-minecraft-version" to minecraftVersion.get())
    }
  }
  shadowJar {
    archiveFileName = productionJarName(minecraftVersion)
    listOf(
      "org.incendo.cloud",
      "io.leangen.geantyref",
      "org.bstats",
      "jakarta.inject",
      "com.google.inject",
      "org.aopalliance",
    ).forEach(::reloc)
  }
  withType<RunServer>().configureEach {
    runProps(layout, providers).forEach { (key, value) ->
      systemProperty(key, value)
    }
  }
}

squaremapPlatform.productionJar = tasks.shadowJar.flatMap { it.archiveFile }

runPaper.folia.registerTask()

paperweight {
  injectPaperRepository = false
  reobfArtifactConfiguration = ReobfArtifactConfiguration.MOJANG_PRODUCTION
}

bukkitPluginYaml {
  name = "squaremap"
  main = "xyz.jpenilla.squaremap.paper.SquaremapPaperBootstrap"
  load = BukkitPluginYaml.PluginLoadOrder.STARTUP
  authors = listOf("jmp")
  website = githubUrl
  apiVersion = plainMinecraftVersion
  foliaSupported = true
}

hangarPublish.publications.register("plugin") {
  version = project.version as String
  id = "squaremap"
  channel = "Release"
  changelog = releaseNotes
  apiKey = providers.environmentVariable("HANGAR_UPLOAD_KEY")
  platforms.paper {
    jar = squaremapPlatform.productionJar
    platformVersions.add(minecraftVersion)
  }
}

publishMods.modrinth {
  minecraftVersions.add(minecraftVersion)
  modLoaders.addAll("paper", "folia")
}
