plugins {
  id("loom-platform-conventions")
}

loom.accessWidenerPath.set(layout.projectDirectory.file("src/main/resources/squaremap-fabric.accesswidener"))

repositories {
  maven("https://maven.ladysnake.org/releases/") {
    mavenContent {
      includeGroup("dev.onyxstudios.cardinal-components-api")
    }
  }
}

dependencies {
  minecraft(libs.minecraft)
  mappings(loom.officialMojangMappings())
  modImplementation(libs.fabricLoader)
  modImplementation(libs.fabricApi)

  squaremap(projects.squaremapCommon) {
    exclude("cloud.commandframework", "cloud-core")
    exclude("cloud.commandframework", "cloud-minecraft-extras")
    exclude("io.leangen.geantyref")
  }

  modImplementation(libs.adventurePlatformFabric)
  include(libs.adventurePlatformFabric)

  modImplementation(libs.cloudFabric)
  include(libs.cloudFabric)

  implementation(libs.cloudMinecraftExtras) {
    isTransitive = false // we depend on adventure separately
  }
  include(libs.cloudMinecraftExtras)

  modImplementation(libs.cardinalComponentsBase)
  include(libs.cardinalComponentsBase)
  modImplementation(libs.cardinalComponentsEntity)
  include(libs.cardinalComponentsEntity)
}

squaremapLoomPlatform.modInfoFilePath.set("fabric.mod.json")

tasks.remapJar {
  archiveFileName.set(productionJarName(libs.versions.minecraft))
}

publishMods.modrinth {
  minecraftVersions.add(libs.versions.minecraft)
  modLoaders.add("fabric")
  requires("fabric-api")
}
