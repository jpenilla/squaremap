plugins {
  id("platform-conventions")
  id("io.papermc.paperweight.userdev")
  id("net.minecrell.plugin-yml.bukkit")
  id("xyz.jpenilla.run-paper")
}

val minecraftVersion = libs.versions.minecraft.get()

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
      "javax.inject",
      "com.google.inject",
      "org.aopalliance",
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
  main = "xyz.jpenilla.squaremap.paper.SquaremapPaper"
  name = rootProject.name
  apiVersion = "1.19"
  website = providers.gradleProperty("githubUrl").get()
  authors = listOf("jmp")
}
