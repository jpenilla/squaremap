import xyz.jpenilla.resourcefactory.fabric.Environment

plugins {
  id("squaremap.platform.loom")
  id("xyz.jpenilla.resource-factory-fabric-convention")
}

loom.accessWidenerPath = layout.projectDirectory.file("src/main/resources/squaremap-fabric.accesswidener")

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

fabricModJson {
  id = rootProject.name
  name = rootProject.name
  author("jmp")
  contact {
    homepage = githubUrl
    sources = githubUrl
    issues = githubUrl + "issues/"
  }
  environment = Environment.ANY
  mainEntrypoint("xyz.jpenilla.squaremap.fabric.SquaremapFabric\$Initializer")
  entrypoint("cardinal-components", "xyz.jpenilla.squaremap.fabric.SquaremapComponentInitializer")
  mitLicense()
  mixin("squaremap-fabric.mixins.json")
  accessWidener = "squaremap-fabric.accesswidener"

  depends("fabric-api", "*")
  depends("fabricloader", ">=${libs.versions.fabricLoader.get()}")
  depends("minecraft", "~1.20.4")
  depends("cloud", "*")
  depends("adventure-platform-fabric", "*")

  custom("cardinal-components", simpleCustomValueList(listOf("squaremap:player_component")))
}

tasks.remapJar {
  archiveFileName = productionJarName(libs.versions.minecraft)
}

publishMods.modrinth {
  minecraftVersions.add(libs.versions.minecraft)
  modLoaders.add("fabric")
  requires("fabric-api")
}
