plugins {
  id("squaremap.platform.loom")
}

loom.accessWidenerPath = layout.projectDirectory.file("src/main/resources/squaremap-fabric.accesswidener")

repositories {
  maven("https://maven.ladysnake.org/releases/") {
    mavenContent {
      includeGroup("org.ladysnake.cardinal-components-api")
    }
  }
}

dependencies {
  minecraft(libs.minecraft)
  mappings(loom.officialMojangMappings())
  modImplementation(libs.fabricLoader)
  modImplementation(libs.fabricApi)

  shade(projects.squaremapCommon) {
    exclude("org.incendo", "cloud-core")
    exclude("org.incendo", "cloud-minecraft-extras")
    exclude("org.incendo", "cloud-processors-confirmation")
    exclude("io.leangen.geantyref")
  }

  modImplementation(libs.adventurePlatformFabric)
  include(libs.adventurePlatformFabric)

  modImplementation(libs.cloudFabric)
  include(libs.cloudFabric)

  modImplementation(libs.fabricPermissionsApi)
  include(libs.fabricPermissionsApi)

  implementation(libs.cloudMinecraftExtras) {
    isTransitive = false // we depend on adventure separately
  }
  include(libs.cloudMinecraftExtras)
  implementation(libs.cloudConfirmation)
  include(libs.cloudConfirmation)
  include(libs.cloudProcessorsCommon)

  modImplementation(libs.cardinalComponentsBase)
  include(libs.cardinalComponentsBase)
  modImplementation(libs.cardinalComponentsEntity)
  include(libs.cardinalComponentsEntity)
}

squaremapPlatform.modInfoFilePath = "fabric.mod.json"

tasks.remapJar {
  archiveFileName = productionJarName(libs.versions.minecraft)
}

publishMods.modrinth {
  minecraftVersions.add(libs.versions.minecraft)
  modLoaders.add("fabric")
  requires("fabric-api")
}
