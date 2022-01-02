plugins {
  `platform-conventions`
  id("io.papermc.paperweight.userdev") version "1.3.3"
  id("net.minecrell.plugin-yml.bukkit") version "0.5.1"
  id("xyz.jpenilla.run-paper") version "1.0.6"
}

dependencies {
  paperDevBundle("1.18.1-R0.1-SNAPSHOT")

  implementation(project(":squaremap-api"))
  implementation(project(":squaremap-common"))

  implementation("cloud.commandframework:cloud-paper")
  implementation("org.bstats:bstats-bukkit:2.2.1")
  implementation("xyz.jpenilla:reflection-remapper:0.1.0-SNAPSHOT")
}

configurations.mojangMappedServer {
  exclude("org.yaml", "snakeyaml")
}

tasks.shadowJar {
  minimize {
    exclude { it.moduleName == "squaremap-api" }
    exclude(dependency("io.undertow:.*:.*")) // does not like being minimized _or_ relocated (xnio errors)
  }
  listOf(
    "cloud.commandframework",
    "io.leangen.geantyref",
    "net.kyori.adventure.text.minimessage",
    "org.bstats",
    "xyz.jpenilla.reflectionremapper",
    "net.fabricmc.mappingio",
    "org.owasp.html",
    "org.spongepowered.configurate",
    "org.yaml.snakeyaml",
  ).forEach { relocate(it, "squaremap.libraries.$it") }
}

squaremapPlatform {
  productionJar.set(tasks.reobfJar.flatMap { it.outputJar })
}

bukkit {
  main = "xyz.jpenilla.squaremap.plugin.SquaremapPlugin"
  name = rootProject.name
  apiVersion = "1.18"
  website = providers.gradleProperty("githubUrl").forUseAtConfigurationTime().get()
  authors = listOf("jmp")
}
