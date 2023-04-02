import io.papermc.hangarpublishplugin.model.Platforms

plugins {
  id("platform-conventions")
  id("io.papermc.paperweight.userdev")
  id("xyz.jpenilla.run-paper")
  id("io.papermc.hangar-publish-plugin")
}

val minecraftVersion = libs.versions.minecraft

dependencies {
  paperweight.devBundle("dev.folia", minecraftVersion.map { "$it-R0.1-SNAPSHOT" }.get())

  implementation(projects.squaremapCommon)

  implementation(libs.cloudPaper)
  implementation(libs.bStatsBukkit)
}

configurations.mojangMappedServer {
  exclude("org.yaml", "snakeyaml")
}

tasks {
  jar {
    manifest {
      attributes("squaremap-target-minecraft-version" to minecraftVersion.get())
    }
  }
  shadowJar {
    listOf(
      "cloud.commandframework",
      "io.leangen.geantyref",
      "org.bstats",
      "javax.inject",
      "com.google.inject",
      "org.aopalliance",
    ).forEach(::reloc)
  }
  reobfJar {
    outputJar.set(productionJarLocation(minecraftVersion))
  }
  processResources {
    val props = mapOf(
      "version" to project.version,
      "website" to providers.gradleProperty("githubUrl").get(),
      "description" to project.description,
      "apiVersion" to "'" + minecraftVersion.get().take(4) + "'",
    )
    inputs.properties(props)
    filesMatching("plugin.yml") {
      expand(props)
    }
  }
}

squaremapPlatform {
  productionJar.set(tasks.reobfJar.flatMap { it.outputJar })
}

runPaper.folia.registerTask()

hangarPublish.publications.register("plugin") {
  version.set(project.version as String)
  owner.set("jmp")
  slug.set("squaremap")
  channel.set("Release")
  changelog.set(releaseNotes)
  apiKey.set(providers.environmentVariable("HANGAR_UPLOAD_KEY"))
  platforms.register(Platforms.PAPER) {
    jar.set(squaremapPlatform.productionJar)
    platformVersions.add(minecraftVersion)
  }
}
