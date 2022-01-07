plugins {
  `platform-conventions`
  id("quiet-fabric-loom") version "0.10-SNAPSHOT"
}

val minecraftVersion = "1.18.1"

val squaremap: Configuration by configurations.creating
configurations.implementation {
  extendsFrom(squaremap)
}

repositories {
  maven("https://ladysnake.jfrog.io/artifactory/mods/") {
    mavenContent {
      includeGroup("io.github.onyxstudios.Cardinal-Components-API")
    }
  }
}

dependencies {
  minecraft("com.mojang", "minecraft", minecraftVersion)
  mappings(loom.officialMojangMappings())
  modImplementation("net.fabricmc", "fabric-loader", "0.12.12")
  modImplementation("net.fabricmc.fabric-api:fabric-api:0.44.0+1.18")

  squaremap(project(":squaremap-common")) {
    exclude("cloud.commandframework", "cloud-core")
    exclude("cloud.commandframework", "cloud-minecraft-extras")
    exclude("io.leangen.geantyref")
  }

  modImplementation("net.kyori:adventure-platform-fabric:5.0.0") {
    exclude("ca.stellardrift", "colonel")
  }
  include("net.kyori:adventure-platform-fabric:5.0.0")

  modImplementation("cloud.commandframework:cloud-fabric:1.7.0-SNAPSHOT")
  include("cloud.commandframework:cloud-fabric:1.7.0-SNAPSHOT")
  include(implementation("cloud.commandframework:cloud-minecraft-extras:1.7.0-SNAPSHOT") {
    isTransitive = false // we depend on adventure separately
  })

  modImplementation("io.github.onyxstudios.Cardinal-Components-API:cardinal-components-base:4.0.1")
  include("io.github.onyxstudios.Cardinal-Components-API:cardinal-components-base:4.0.1")
  modImplementation("io.github.onyxstudios.Cardinal-Components-API:cardinal-components-entity:4.0.1")
  include("io.github.onyxstudios.Cardinal-Components-API:cardinal-components-entity:4.0.1")
}

squaremapPlatform {
  productionJar.set(tasks.remapJar.flatMap { it.archiveFile })
}

loom {
  accessWidenerPath.set(layout.projectDirectory.file("src/main/resources/squaremap-fabric.accesswidener"))
}

tasks {
  shadowJar {
    configurations = listOf(squaremap)
  }
  processResources {
    val props = mapOf(
      "version" to project.version,
      "github_url" to rootProject.providers.gradleProperty("githubUrl")
        .forUseAtConfigurationTime().get(),
      "description" to project.description,
    )
    inputs.properties(props)
    filesMatching("fabric.mod.json") {
      expand(props)
    }
  }
  remapJar {
    input.set(shadowJar.flatMap { it.archiveFile })
    archiveFileName.set("${project.name}-mc$minecraftVersion-${project.version}.jar")
  }
}
