import xyz.jpenilla.resourcefactory.fabric.Environment

plugins {
  id("squaremap.platform.loom")
  alias(libs.plugins.resource.factory.fabric)
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

tasks.remapJar {
  archiveFileName = productionJarName(libs.versions.minecraft)
}

tasks.withType<net.fabricmc.loom.task.AbstractRunTask>().configureEach {
  runProps(layout, providers).forEach { (key, value) ->
    systemProperty(key, value)
  }
}

fabricModJson {
  id = "squaremap"
  name = "squaremap"
  author("jmp")
  contact {
    homepage = githubUrl
    sources = githubUrl
    issues = githubUrl.map { "${it}issues/" }
  }
  environment = Environment.ANY
  mainEntrypoint("xyz.jpenilla.squaremap.fabric.SquaremapFabricInitializer")
  entrypoint("cardinal-components", "xyz.jpenilla.squaremap.fabric.SquaremapComponentInitializer")
  custom.put("cardinal-components", simpleCustomValueList(listOf("squaremap:player_component")))
  mitLicense()
  mixin("squaremap-fabric.mixins.json")
  accessWidener = "squaremap-fabric.accesswidener"
  depends("fabric-api", "*")
  depends("fabricloader", ">=0.16.7")
  depends("minecraft", libs.versions.minecraft.get())
  depends("cloud", "*")
  depends("adventure-platform-fabric", "*")
}

publishMods.modrinth {
  minecraftVersions.add(libs.versions.minecraft)
  modLoaders.add("fabric")
  requires("fabric-api")
}
