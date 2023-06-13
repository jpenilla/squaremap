import io.papermc.hangarpublishplugin.model.Platforms

plugins {
  id("platform-conventions")
  id("io.papermc.paperweight.userdev")
  id("net.minecrell.plugin-yml.bukkit")
  id("xyz.jpenilla.run-paper")
  id("io.papermc.hangar-publish-plugin")
}

val minecraftVersion = libs.versions.minecraft

dependencies {
  paperweight.paperDevBundle(minecraftVersion.map { "$it-R0.1-SNAPSHOT" })

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
    filesMatching("plugin.yml") {
      filter { it.replace("1.20", "'1.20'") }
    }
  }
}

squaremapPlatform {
  productionJar.set(tasks.reobfJar.flatMap { it.outputJar })
}

bukkit {
  main = "xyz.jpenilla.squaremap.paper.SquaremapPaperBootstrap"
  name = rootProject.name
  apiVersion = "1.20"
  website = providers.gradleProperty("githubUrl").get()
  authors = listOf("jmp")
}

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
