plugins {
  `platform-conventions`
  id("io.papermc.paperweight.userdev") version "1.3.3"
  id("net.minecrell.plugin-yml.bukkit") version "0.5.1"
  id("xyz.jpenilla.run-paper") version "1.0.6"
}

val minecraftVersion = "1.18.1"

dependencies {
  paperDevBundle("$minecraftVersion-R0.1-SNAPSHOT")

  implementation(project(":squaremap-api"))
  implementation(project(":squaremap-common"))

  implementation("cloud.commandframework:cloud-paper")
  implementation("org.bstats:bstats-bukkit:2.2.1")
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
