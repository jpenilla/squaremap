import io.papermc.paperweight.userdev.ReobfArtifactConfiguration

plugins {
  id("squaremap.platform")
  id("io.papermc.paperweight.userdev")
  alias(libs.plugins.run.paper)
  alias(libs.plugins.hangar.publish)
}

val minecraftVersion = libs.versions.minecraft

dependencies {
  paperweight.paperDevBundle(minecraftVersion.map { "$it-R0.1-SNAPSHOT" })

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
  processResources {
    expandIn("plugin.yml", mapOf(
      "version" to project.version,
      "website" to providers.gradleProperty("githubUrl").get(),
      "description" to project.description,
      "apiVersion" to minecraftVersion.get(),
    ))
  }
}

squaremapPlatform.productionJar = tasks.shadowJar.flatMap { it.archiveFile }

runPaper.folia.registerTask()

paperweight {
  injectPaperRepository = false
  reobfArtifactConfiguration = ReobfArtifactConfiguration.MOJANG_PRODUCTION
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
