import me.modmuss50.mpp.platforms.modrinth.ModrinthEnvironment
import xyz.jpenilla.resourcefactory.fabric.Environment

plugins {
  id("squaremap.platform.loom")
  alias(libs.plugins.resource.factory.fabric)
}

loom.accessWidenerPath = layout.projectDirectory.file("src/main/resources/squaremap-fabric.accesswidener")

dependencies {
  minecraft(libs.minecraft)
  implementation(libs.fabricLoader)
  implementation(libs.fabricApi)

  shade(projects.squaremapCommon) {
    exclude("org.incendo", "cloud-core")
    exclude("org.incendo", "cloud-minecraft-extras")
    exclude("org.incendo", "cloud-processors-confirmation")
    exclude("io.leangen.geantyref")
  }

  implementation(libs.adventurePlatformFabric)
  include(libs.adventurePlatformFabric)

  implementation(libs.cloudFabric)
  include(libs.cloudFabric)

  implementation(libs.fabricPermissionsApi)
  include(libs.fabricPermissionsApi)

  implementation(libs.cloudMinecraftExtras) {
    isTransitive = false // we depend on adventure separately
  }
  include(libs.cloudMinecraftExtras)
  implementation(libs.cloudConfirmation)
  include(libs.cloudConfirmation)
  include(libs.cloudProcessorsCommon)
}

tasks.shadowJar {
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
  environment = ModrinthEnvironment.SERVER_ONLY_CLIENT_OPTIONAL
  minecraftVersions.add(libs.versions.minecraft)
  modLoaders.add("fabric")
  requires("fabric-api")
}
