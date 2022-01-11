plugins {
  `platform-conventions`
  id("io.papermc.paperweight.userdev")
  id("net.minecrell.plugin-yml.bukkit")
  id("xyz.jpenilla.run-paper")
}

val minecraftVersion = libs.versions.minecraft.forUseAtConfigurationTime().get()

dependencies {
  paperDevBundle("$minecraftVersion-R0.1-SNAPSHOT")

  implementation(projects.squaremapCommon)

  implementation(libs.cloudPaper)
  implementation(libs.bStatsBukkit)
}

configurations.mojangMappedServer {
  exclude("org.yaml", "snakeyaml")
}

tasks {
  shadowJar {
    listOf(
      "cloud.commandframework",
      "io.leangen.geantyref",
      "org.bstats",
    ).forEach(::reloc)
  }
  reobfJar {
    outputJar.set(layout.buildDirectory.file("libs/${base.archivesName.get()}-mc$minecraftVersion-$version.jar"))
  }
}

squaremapPlatform {
  productionJar.set(tasks.reobfJar.flatMap { it.outputJar })
}

bukkit {
  main = "xyz.jpenilla.squaremap.paper.SquaremapPlugin"
  name = rootProject.name
  apiVersion = "1.18"
  website = providers.gradleProperty("githubUrl").forUseAtConfigurationTime().get()
  authors = listOf("jmp")
}
